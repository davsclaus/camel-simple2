/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.language.simple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.Expression;
import org.apache.camel.Predicate;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.language.simple.ast.BinaryOperator;
import org.apache.camel.language.simple.ast.DoubleQuoteEnd;
import org.apache.camel.language.simple.ast.DoubleQuoteStart;
import org.apache.camel.language.simple.ast.FunctionEnd;
import org.apache.camel.language.simple.ast.FunctionStart;
import org.apache.camel.language.simple.ast.Literal;
import org.apache.camel.language.simple.ast.LiteralNode;
import org.apache.camel.language.simple.ast.LogicalOperator;
import org.apache.camel.language.simple.ast.NullNode;
import org.apache.camel.language.simple.ast.SimpleNode;
import org.apache.camel.language.simple.ast.SingleQuoteEnd;
import org.apache.camel.language.simple.ast.SingleQuoteStart;
import org.apache.camel.language.simple.ast.UnaryOperator;

/**
 * A parser to parse simple language as a Camel {@link Predicate}
 */
public class SimplePredicateParser extends BaseSimpleParser {

    public SimplePredicateParser(String expression) {
        super(expression);
    }

    public Predicate parsePredicate() {
        clear();
        try {
            return doParsePredicate();
        } catch (SimpleParserException e) {
            // catch parser exception and turn that into a syntax exceptions
            throw new SimpleIllegalSyntaxException(expression, e.getIndex(), e.getMessage(), e);
        } catch (Exception e) {
            // include exception in rethrown exception
            throw new SimpleIllegalSyntaxException(expression, -1, e.getMessage(), e);
        }
    }

    protected Predicate doParsePredicate() {

        // parse using the following grammar
        nextToken();
        while (!token.getType().isEol()) {
            // predicate supports quotes, functions, operators and whitespaces
            if (!singleQuotedText() && !doubleQuotedText() && !functionText() && !unaryOperator() && !binaryOperator() && !logicalOperator()
                    && !token.getType().isWhitespace() && !token.getType().isEol()) {
                // okay the symbol was not one of the above, so its not supported
                // use the previous index as that is where the problem is
                throw new SimpleParserException("unexpected " + token.getType().getType() + " token", previousIndex);
            }
            nextToken();
        }

        // now after parsing we need a bit of work to do, to make it easier to turn the tokens
        // into and ast, and then from the ast, to Camel predicate(s).
        // hence why there is a number of tasks going on below to accomplish this

        // remove any ignorable white space tokens
        removeIgnorableWhiteSpaceTokens();
        // turn the tokens into the ast model
        parseTokensAndCreateNodes();
        // compact and stack blocks (eg function start/end, quotes start/end, etc.)
        prepareBlocks();
        // compact and stack unary operators
        prepareUnaryOperators();
        // compact and stack binary operators
        prepareBinaryOperators();
        // compact and stack logical operators
        prepareLogicalOperators();

        // create and return as a Camel predicate
        List<Predicate> predicates = createPredicates();
        if (predicates.isEmpty()) {
            return null;
        } else if (predicates.size() == 1) {
            return predicates.get(0);
        } else {
            return PredicateHelper.and(predicates);
        }
    }

    /**
     * Parses the tokens and crates the AST nodes.
     * <p/>
     * After the initial parsing of the input (input -> tokens) then we
     * parse again (tokens -> ast).
     * <p/>
     * In this parsing the balance of the blocks is checked, so that each block has a matching
     * start and end token. For example a single quote block, or a function block etc.
     */
    protected void parseTokensAndCreateNodes() {
        // we loop the tokens and create a sequence of ast nodes

        // we need to keep a bit of state for keeping track of single and double quotes
        // which need to be balanced and have matching start/end pairs
        SimpleNode lastSingle = null;
        SimpleNode lastDouble = null;
        SimpleNode lastFunction = null;
        AtomicBoolean startSingle = new AtomicBoolean(false);
        AtomicBoolean startDouble = new AtomicBoolean(false);
        AtomicBoolean startFunction = new AtomicBoolean(false);

        LiteralNode imageToken = null;
        for (SimpleToken token : tokens) {
            // break if eol
            if (token.getType().isEol()) {
                break;
            }

            // create a node from the token
            SimpleNode node = createNode(token, startSingle, startDouble, startFunction);
            if (node != null) {
                // keep state of last single/double
                if (node instanceof SingleQuoteStart) {
                    lastSingle = node;
                } else if (node instanceof DoubleQuoteStart) {
                    lastDouble = node;
                } else if (node instanceof FunctionStart) {
                    lastFunction = node;
                }

                // a new token was created so the current image token need to be added first
                if (imageToken != null) {
                    nodes.add(imageToken);
                    imageToken = null;
                }
                // and then add the created node
                nodes.add(node);
                // continue to next
                continue;
            }

            // if no token was created then its a character/whitespace/escaped symbol
            // which we need to add together in the same image
            if (imageToken == null) {
                imageToken = new Literal(token);
            }
            imageToken.addText(token.getText());
        }

        // append any leftover image tokens (when we reached eol)
        if (imageToken != null) {
            nodes.add(imageToken);
        }

        // validate the single, double quote pairs and functions is in balance
        if (startSingle.get()) {
            int index = lastSingle != null ? lastSingle.getToken().getIndex() : 0;
            throw new SimpleParserException("single quote has no ending quote", index);
        }
        if (startDouble.get()) {
            int index = lastDouble != null ? lastDouble.getToken().getIndex() : 0;
            throw new SimpleParserException("double quote has no ending quote", index);
        }
        if (startFunction.get()) {
            // we have a start function, but no ending function
            int index = lastFunction != null ? lastFunction.getToken().getIndex() : 0;
            throw new SimpleParserException("function has no ending token", index);
        }
    }


    /**
     * Creates a node from the given token
     *
     * @param token         the token
     * @param startSingle   state of single quoted blocks
     * @param startDouble   state of double quoted blocks
     * @param startFunction state of function blocks
     * @return the created node, or <tt>null</tt> to let a default node be created instead.
     */
    private SimpleNode createNode(SimpleToken token, AtomicBoolean startSingle, AtomicBoolean startDouble,
                                  AtomicBoolean startFunction) {
        if (token.getType().isFunctionStart()) {
            startFunction.set(true);
            return new FunctionStart(token);
        } else if (token.getType().isFunctionEnd()) {
            startFunction.set(false);
            return new FunctionEnd(token);
        }

        // if we are inside a function, then we do not support any other kind of tokens
        // as we want all the tokens to be literal instead
        if (startFunction.get()) {
            return null;
        }

        // okay so far we also want to support quotes
        if (token.getType().isSingleQuote()) {
            SimpleNode answer;
            boolean start = startSingle.get();
            if (!start) {
                answer = new SingleQuoteStart(token);
            } else {
                answer = new SingleQuoteEnd(token);
            }
            // flip state on start/end flag
            startSingle.set(!start);
            return answer;
        } else if (token.getType().isDoubleQuote()) {
            SimpleNode answer;
            boolean start = startDouble.get();
            if (!start) {
                answer = new DoubleQuoteStart(token);
            } else {
                answer = new DoubleQuoteEnd(token);
            }
            // flip state on start/end flag
            startDouble.set(!start);
            return answer;
        }

        // if we are inside a quote, then we do not support any further kind of tokens
        // as we want to only support embedded functions and all other kinds to be literal tokens
        if (startSingle.get() || startDouble.get()) {
            return null;
        }

        // okay we are not inside a function or quote, so we want to support operators
        // and the special null value as well
        if (token.getType().isUnary()) {
            return new UnaryOperator(token);
        } else if (token.getType().isBinary()) {
            return new BinaryOperator(token);
        } else if (token.getType().isLogical()) {
            return new LogicalOperator(token);
        } else if (token.getType().isNullValue()) {
            return new NullNode(token);
        }

        // by returning null, we will let the parser determine what to do
        return null;
    }

    /**
     * Removes any ignorable whitespace tokens.
     * <p/>
     * During the initial parsing (input -> tokens), then there may
     * be excessive whitespace tokens, which can safely be removed,
     * which makes the succeeding parsing easier.
     */
    private void removeIgnorableWhiteSpaceTokens() {
        // white space can be removed if its not part of a quoted text
        boolean quote = false;

        Iterator<SimpleToken> it = tokens.iterator();
        while (it.hasNext()) {
            SimpleToken token = it.next();
            if (token.getType().isSingleQuote()) {
                quote = !quote;
            } else if (token.getType().isWhitespace() && !quote) {
                it.remove();
            }
        }
    }

    /**
     * Prepares binary operators.
     * <p/>
     * This process prepares the binary operators in the AST. This is done
     * by linking the binary operator with both the right and left hand side
     * nodes, to have the AST graph updated and prepared properly.
     * <p/>
     * So when the AST node is later used to create the {@link Predicate}s
     * to be used by Camel then the AST graph has a linked and prepared
     * graph of nodes which represent the input expression.
     */
    private void prepareBinaryOperators() {
        Stack<SimpleNode> stack = new Stack<SimpleNode>();

        SimpleNode left = null;
        for (int i = 0; i < nodes.size(); i++) {
            if (left == null) {
                left = i > 0 ? nodes.get(i - 1) : null;
            }
            SimpleNode token = nodes.get(i);
            SimpleNode right = i < nodes.size() - 1 ? nodes.get(i + 1) : null;

            if (token instanceof BinaryOperator) {
                BinaryOperator binary = (BinaryOperator) token;
                if (left == null) {
                    throw new SimpleParserException("no preceding token to use with binary operator ", token.getToken().getIndex());
                }
                if (!binary.acceptLeftNode(left)) {
                    throw new SimpleParserException("preceding token not applicable to use with binary operator", token.getToken().getIndex());
                }
                if (right == null) {
                    throw new SimpleParserException("no succeeding token to use with binary operator", token.getToken().getIndex());
                }
                if (!binary.acceptRightNode(right)) {
                    throw new SimpleParserException("succeeding token not applicable to use with binary operator", token.getToken().getIndex());
                }

                // pop previous as we need to replace it with this binary operator
                stack.pop();
                stack.push(token);
                // advantage after the right hand side
                i++;
                // this token is now the left for the next loop
                left = token;
            } else {
                // clear left
                left = null;
                stack.push(token);
            }
        }

        nodes.clear();
        nodes.addAll(stack);
    }

    /**
     * Prepares logical operators.
     * <p/>
     * This process prepares the logical operators in the AST. This is done
     * by linking the logical operator with both the right and left hand side
     * nodes, to have the AST graph updated and prepared properly.
     * <p/>
     * So when the AST node is later used to create the {@link Predicate}s
     * to be used by Camel then the AST graph has a linked and prepared
     * graph of nodes which represent the input expression.
     */
    private void prepareLogicalOperators() {
        Stack<SimpleNode> stack = new Stack<SimpleNode>();

        SimpleNode left = null;
        for (int i = 0; i < nodes.size(); i++) {
            if (left == null) {
                left = i > 0 ? nodes.get(i - 1) : null;
            }
            SimpleNode token = nodes.get(i);
            SimpleNode right = i < nodes.size() - 1 ? nodes.get(i + 1) : null;

            if (token instanceof LogicalOperator) {
                LogicalOperator logical = (LogicalOperator) token;
                if (left == null) {
                    throw new SimpleParserException("no preceding token to use with logical operator ", token.getToken().getIndex());
                }
                if (!logical.acceptLeftNode(left)) {
                    throw new SimpleParserException("preceding token not applicable to use with logical operator", token.getToken().getIndex());
                }
                if (right == null) {
                    throw new SimpleParserException("no succeeding token to use with logical operator", token.getToken().getIndex());
                }
                if (!logical.acceptRightNode(right)) {
                    throw new SimpleParserException("succeeding token not applicable to use with logical operator", token.getToken().getIndex());
                }

                // pop previous as we need to replace it with this binary operator
                stack.pop();
                stack.push(token);
                // advantage after the right hand side
                i++;
                // this token is now the left for the next loop
                left = token;
            } else {
                // clear left
                left = null;
                stack.push(token);
            }
        }

        nodes.clear();
        nodes.addAll(stack);
    }

    /**
     * Creates the {@link Predicate}s from the AST nodes.
     *
     * @return the created {@link Predicate}s, is never <tt>null</tt>.
     */
    private List<Predicate> createPredicates() {
        List<Predicate> answer = new ArrayList<Predicate>();
        for (SimpleNode node : nodes) {
            Expression exp = node.createExpression(expression);
            if (exp != null) {
                Predicate predicate = PredicateBuilder.toPredicate(exp);
                answer.add(predicate);
            }
        }
        return answer;
    }

    // --------------------------------------------------------------
    // grammar
    // --------------------------------------------------------------

    // the predicate parser understands a lot more than the expression parser
    // - single quoted = block of nodes enclosed by single quotes
    // - double quoted = block of nodes enclosed by double quotes
    // - function = simple functions such as ${body} etc
    // - numeric = numeric value
    // - boolean = boolean value
    // - null = null value
    // - unary operator = operator attached to the left hand side node
    // - binary operator = operator attached to both the left and right hand side nodes
    // - logical operator = operator attached to both the left and right hand side nodes

    protected boolean singleQuotedText() {
        if (accept(TokenType.singleQuote)) {
            nextToken();
            while (!token.getType().isSingleQuote() && !token.getType().isEol()) {
                // we need to loop until we find the ending single quote, or the eol
                nextToken();
            }
            expect(TokenType.singleQuote);
            return true;
        }
        return false;
    }

    protected boolean doubleQuotedText() {
        if (accept(TokenType.doubleQuote)) {
            nextToken();
            while (!token.getType().isDoubleQuote() && !token.getType().isEol()) {
                // we need to loop until we find the ending double quote, or the eol
                nextToken();
            }
            expect(TokenType.doubleQuote);
            return true;
        }
        return false;
    }

    protected boolean functionText() {
        if (accept(TokenType.functionStart)) {
            nextToken();
            while (!token.getType().isFunctionEnd() && !token.getType().isEol()) {
                // we need to loop until we find the ending function quote, or the eol
                nextToken();
            }
            expect(TokenType.functionEnd);
            return true;
        }
        return false;
    }

    protected boolean unaryOperator() {
        if (accept(TokenType.unaryOperator)) {
            nextToken();
            // there should be a whitespace after the operator
            expect(TokenType.whiteSpace);
            return true;
        }
        return false;
    }

    protected boolean binaryOperator() {
        if (accept(TokenType.binaryOperator)) {
            nextToken();
            // there should be at least one whitespace after the operator
            expectAndAcceptMore(TokenType.whiteSpace);

            // then we expect either some quoted text, another function, or a numeric, boolean or null value
            if (singleQuotedText() || doubleQuotedText() || functionText() || numericValue() || booleanValue() || nullValue()) {
                // then after the right hand side value, there should be a whitespace if there is more tokens
                nextToken();
                if (!token.getType().isEol()) {
                    expect(TokenType.whiteSpace);
                }
            } else {
                throw new SimpleParserException(token.getType() + " not supported by binary operator", token.getIndex());
            }
            return true;
        }
        return false;
    }

    protected boolean logicalOperator() {
        if (accept(TokenType.logicalOperator)) {
            nextToken();
            // there should be at least one whitespace after the operator
            expectAndAcceptMore(TokenType.whiteSpace);

            // then we expect either some quoted text, another function, or a numeric, boolean or null value
            if (singleQuotedText() || doubleQuotedText() || functionText() || numericValue() || booleanValue() || nullValue()) {
                // then after the right hand side value, there should be a whitespace if there is more tokens
                nextToken();
                if (!token.getType().isEol()) {
                    expect(TokenType.whiteSpace);
                }
            } else {
                throw new SimpleParserException(token.getType() + " not supported by logical operator", token.getIndex());
            }
            return true;
        }
        return false;
    }

    protected boolean numericValue() {
        if (accept(TokenType.numericValue)) {
            // no other tokens to check so do not use nextToken
            return true;
        }
        return false;
    }

    protected boolean booleanValue() {
        if (accept(TokenType.booleanValue)) {
            // no other tokens to check so do not use nextToken
            return true;
        }
        return false;
    }

    protected boolean nullValue() {
        if (accept(TokenType.nullValue)) {
            // no other tokens to check so do not use nextToken
            return true;
        }
        return false;
    }

}
