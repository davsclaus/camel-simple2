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
import java.util.List;
import java.util.Stack;

import org.apache.camel.language.simple.ast.Block;
import org.apache.camel.language.simple.ast.BlockEnd;
import org.apache.camel.language.simple.ast.BlockStart;
import org.apache.camel.language.simple.ast.SimpleNode;
import org.apache.camel.language.simple.ast.UnaryOperator;

/**
 * Base class for Simple language parser.
 * <p/>
 * This parser is based on the principles of a
 * <a href="http://en.wikipedia.org/wiki/Recursive_descent_parser">recursive descent parser</a>.
 */
public abstract class BaseSimpleParser {

    protected final String expression;
    protected final SimpleTokenizer tokenizer;
    protected final List<SimpleToken> tokens = new ArrayList<SimpleToken>();
    protected final List<SimpleNode> nodes = new ArrayList<SimpleNode>();
    protected SimpleToken token;
    protected int previousIndex;
    protected int index;

    protected BaseSimpleParser(String expression) {
        this.expression = expression;
        this.tokenizer = new SimpleTokenizer();
    }

    protected void nextToken() {
        if (index < expression.length()) {
            SimpleToken next = tokenizer.nextToken(expression, index);
            // add token
            tokens.add(next);
            token = next;
            // position index after the token
            previousIndex = index;
            index += next.getLength();
        } else {
            // end of tokens
            token = new SimpleToken(new SimpleTokenType(TokenType.eol, null), index);
        }
    }

    protected void clear() {
        token = null;
        previousIndex = 0;
        index = 0;
        tokens.clear();
        nodes.clear();
    }

    /**
     * Stacks the blocks.
     * <p/>
     * This method is needed after the initial parsing of the input according to the grammar.
     */
    protected void stackBlocks() {
        List<SimpleNode> answer = new ArrayList<SimpleNode>();
        Stack<Block> stack = new Stack<Block>();

        for (SimpleNode token : nodes) {
            if (token instanceof BlockStart) {
                // a new block is started, so push on the stack
                stack.push((Block) token);
            } else if (token instanceof BlockEnd) {
                // end block is just an abstract mode, so we should not add it
                Block top = stack.pop();
                answer.add(top);
            } else {
                // if there is a model on the stack then it should accept the child model
                Block block = stack.isEmpty() ? null : stack.peek();
                if (block != null) {
                    if (!block.acceptAndAddNode(token)) {
                        throw new SimpleParserException(block.getToken().getType() + " cannot accept " + token.getToken().getType(), token.getToken().getIndex());
                    }
                } else {
                    // no block, so add to answer
                    answer.add(token);
                }
            }
        }

        // replace tokens from the stack
        nodes.clear();
        nodes.addAll(answer);
    }

    /**
     * Stacks the unary operators
     * <p/>
     * This method is needed after the initial parsing of the input according to the grammar.
     */
    protected void stackUnaryOperators() {
        Stack<SimpleNode> stack = new Stack<SimpleNode>();

        for (SimpleNode node : nodes) {
            if (node instanceof UnaryOperator) {
                UnaryOperator unary = (UnaryOperator) node;
                SimpleNode previous = stack.isEmpty() ? null : stack.pop();
                if (previous == null) {
                    throw new SimpleParserException("no preceding token to use with unary operator", unary.getToken().getIndex());
                } else {
                    unary.acceptLeft(previous);
                }
            }
            stack.push(node);
        }

        nodes.clear();
        nodes.addAll(stack);
    }

    // --------------------------------------------------------------
    // grammar
    // --------------------------------------------------------------

    /**
     * Accept the given token.
     * <p/>
     * This is to be used by the grammar to accept tokens and then continue parsing
     * using the grammar, such as a function grammar.
     *
     * @param accept  the token
     * @return <tt>true</tt> if accepted, <tt>false</tt> otherwise.
     */
    protected boolean accept(TokenType accept) {
        if (token == null || token.getType().getType() == accept) {
            nextToken();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Expect a given token
     *
     * @param expect the token to expect
     * @throws SimpleParserException is thrown if the token is not as expected
     */
    protected void expect(TokenType expect) throws SimpleParserException {
        if (token != null && token.getType().getType() == expect) {
            return;
        } else if (token == null) {
            // use the previous index as that is where the problem is
            throw new SimpleParserException("expected symbol " + expect + " but reached eol", previousIndex);
        } else {
            // use the previous index as that is where the problem is
            throw new SimpleParserException("expected symbol " + expect + " but was " + token.getType().getType(), previousIndex);
        }
    }

    /**
     * Expect and accept a given number of tokens in sequence.
     * <p/>
     * This is used to accept whitespace or string literals.
     *
     * @param expect the token to accept
     */
    protected void expectAndAcceptMore(TokenType expect) {
        expect(expect);

        while (!token.getType().isEol() && token.getType().getType() == expect) {
            nextToken();
        }
    }
}
