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

import org.apache.camel.Expression;
import org.apache.camel.builder.ExpressionBuilder;
import org.apache.camel.language.simple.ast.FunctionEnd;
import org.apache.camel.language.simple.ast.FunctionStart;
import org.apache.camel.language.simple.ast.Literal;
import org.apache.camel.language.simple.ast.LiteralNode;
import org.apache.camel.language.simple.ast.SimpleNode;

/**
 * A parser to parse simple language as a Camel {@link Expression}
 */
public class SimpleExpressionParser extends BaseSimpleParser {

    public SimpleExpressionParser(String expression) {
        super(expression);
    }

    public Expression parseExpression() {
        clear();
        try {
            return doParseExpression();
        } catch (SimpleParserException e) {
            // catch parser exception and turn that into a syntax exceptions
            throw new SimpleIllegalSyntaxException(expression, e.getIndex(), e.getMessage(), e);
        } catch (Exception e) {
            // include exception in rethrown exception
            throw new SimpleIllegalSyntaxException(expression, -1, e.getMessage(), e);
        }
    }

    protected Expression doParseExpression() {
        // parse the expression using the following grammar
        nextToken();
        while (!token.getType().isEol()) {
            // an expression supports just template (eg text) and functions
            templateText();
            functionText();
            nextToken();
        }

        // now after parsing we need a bit of work to do, to make it easier to turn the tokens
        // into and ast, and then from the ast, to Camel expression(s).
        // hence why there is a number of tasks going on below to accomplish this

        // turn the tokens into the ast model
        parseAndCreateAstModel();
        // compact and stack blocks (eg function start/end)
        stackBlocks();

        // create and return as a Camel expression
        List<Expression> expressions = createExpressions();
        if (expressions.isEmpty()) {
            return null;
        } else if (expressions.size() == 1) {
            return expressions.get(0);
        } else {
            // concat expressions as evaluating an expression is like a template language
            return ExpressionBuilder.concatExpression(expressions, expression);
        }
    }

    protected void parseAndCreateAstModel() {
        // we loop the tokens and create a sequence of ast nodes

        LiteralNode imageToken = null;
        for (SimpleToken token : tokens) {
            // break if eol
            if (token.getType().isEol()) {
                break;
            }

            // create a node from the token
            SimpleNode node = createNode(token);
            if (node != null) {
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
    }

    private SimpleNode createNode(SimpleToken token) {
        // expression only support functions
        if (token.getType().isFunctionStart()) {
            return new FunctionStart(token);
        } else if (token.getType().isFunctionEnd()) {
            return new FunctionEnd(token);
        }

        // by returning null, we will let the parser determine what to do
        return null;
    }

    private List<Expression> createExpressions() {
        List<Expression> answer = new ArrayList<Expression>();
        for (SimpleNode token : nodes) {
            Expression exp = token.createExpression(expression);
            if (exp != null) {
                answer.add(exp);
            }
        }
        return answer;
    }

    // --------------------------------------------------------------
    // grammar
    // --------------------------------------------------------------

    // the expression parser only understands
    // - template = literal texts with can contain embedded functions
    // - function = simple functions such as ${body} etc

    protected void templateText() {
        // for template we accept anything but functions
        while (!token.getType().isFunctionStart() && !token.getType().isFunctionEnd() && !token.getType().isEol()) {
            nextToken();
        }
    }

    protected boolean functionText() {
        if (accept(TokenType.functionStart)) {
            while (!token.getType().isFunctionEnd() && !token.getType().isEol()) {
                // we need to loop until we find the ending function quote, or the eol
                nextToken();
            }
            expect(TokenType.functionEnd);
            return true;
        }
        return false;
    }

}
