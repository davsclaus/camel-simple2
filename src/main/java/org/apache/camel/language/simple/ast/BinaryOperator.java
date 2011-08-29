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
package org.apache.camel.language.simple.ast;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Predicate;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.language.simple.BinaryOperatorType;
import org.apache.camel.language.simple.SimpleIllegalSyntaxException;
import org.apache.camel.language.simple.SimpleParserException;
import org.apache.camel.language.simple.SimpleToken;
import org.apache.camel.util.ObjectHelper;

/**
 * Represents a binary operator in the AST.
 */
public class BinaryOperator extends BaseSimpleNode {

    private final BinaryOperatorType operator;
    private SimpleNode left;
    private SimpleNode right;

    public BinaryOperator(SimpleToken token) {
        super(token);
        operator = BinaryOperatorType.asOperator(token.getText());
    }

    @Override
    public String toString() {
        return left + " " + symbol.getText() + " " + right;
    }

    public boolean acceptLeftNode(SimpleNode lef) {
        this.left = lef;
        return true;
    }

    public boolean acceptRightNode(SimpleNode right) {
        this.right = right;
        return true;
    }

    @Override
    public Expression createExpression(String expression) {
        ObjectHelper.notNull(left, "left node", this);
        ObjectHelper.notNull(right, "right node", this);

        final Expression leftExp = left.createExpression(expression);
        final Expression rightExp = right.createExpression(expression);

        if (operator == BinaryOperatorType.EQ) {
            return createExpression(leftExp, rightExp, PredicateBuilder.isEqualTo(leftExp, rightExp));
        } else if (operator == BinaryOperatorType.GT) {
            return createExpression(leftExp, rightExp, PredicateBuilder.isGreaterThan(leftExp, rightExp));
        } else if (operator == BinaryOperatorType.GTE) {
            return createExpression(leftExp, rightExp, PredicateBuilder.isGreaterThanOrEqualTo(leftExp, rightExp));
        } else if (operator == BinaryOperatorType.LT) {
            return createExpression(leftExp, rightExp, PredicateBuilder.isLessThan(leftExp, rightExp));
        } else if (operator == BinaryOperatorType.LTE) {
            return createExpression(leftExp, rightExp, PredicateBuilder.isLessThanOrEqualTo(leftExp, rightExp));
        } else if (operator == BinaryOperatorType.NOT_EQ) {
            return createExpression(leftExp, rightExp, PredicateBuilder.isNotEqualTo(leftExp, rightExp));
        } else if (operator == BinaryOperatorType.IS || operator == BinaryOperatorType.NOT_IS) {
            return createIsExpression(expression, leftExp, rightExp);
        }

        throw new SimpleParserException("Unknown binary operator " + operator, symbol.getIndex());
    }

    private Expression createIsExpression(final String expression, final Expression leftExp, final Expression rightExp) {
        return new Expression() {
            @Override
            public <T> T evaluate(Exchange exchange, Class<T> type) {
                Predicate predicate;
                String name = rightExp.evaluate(exchange, String.class);
                if (name == null || "null".equals(name)) {
                    throw new SimpleIllegalSyntaxException(expression, right.getToken().getIndex(), operator + " operator cannot accept null. A class type must be provided.");
                }
                Class<?> rightType = exchange.getContext().getClassResolver().resolveClass(name);
                if (rightType == null) {
                    throw new SimpleIllegalSyntaxException(expression, left.getToken().getIndex(), operator + " operator cannot find class with name: " + name);
                }

                predicate = PredicateBuilder.isInstanceOf(leftExp, rightType);
                if (operator == BinaryOperatorType.NOT_IS) {
                    predicate = PredicateBuilder.not(predicate);
                }
                boolean answer = predicate.matches(exchange);

                return exchange.getContext().getTypeConverter().convertTo(type, answer);
            }

            @Override
            public String toString() {
                return left + " " + symbol.getText() + " " + right;
            }
        };
    }

    private Expression createExpression(final Expression left, final Expression right, final Predicate predicate) {
        return new Expression() {
            @Override
            public <T> T evaluate(Exchange exchange, Class<T> type) {
                boolean answer = predicate.matches(exchange);
                return exchange.getContext().getTypeConverter().convertTo(type, answer);
            }

            @Override
            public String toString() {
                return left + " " + symbol.getText() + " " + right;
            }
        };
    }

}
