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

/**
 * Tokenizer to create {@link SimpleToken} from the input.
 */
public class SimpleTokenizer {

    private final List<SimpleTokenType> knownTokens = new ArrayList<SimpleTokenType>();

    public SimpleTokenizer() {
        // add known tokens
        knownTokens.add(new SimpleTokenType(TokenType.whiteSpace, " "));
        knownTokens.add(new SimpleTokenType(TokenType.singleQuote, "'"));
        knownTokens.add(new SimpleTokenType(TokenType.doubleQuote, "\""));
        knownTokens.add(new SimpleTokenType(TokenType.functionStart, "${"));
        knownTokens.add(new SimpleTokenType(TokenType.functionStart, "$simple{"));
        knownTokens.add(new SimpleTokenType(TokenType.functionEnd, "}"));
        knownTokens.add(new SimpleTokenType(TokenType.booleanValue, "true"));
        knownTokens.add(new SimpleTokenType(TokenType.booleanValue, "false"));
        knownTokens.add(new SimpleTokenType(TokenType.nullValue, "null"));

        // binary operators
        knownTokens.add(new SimpleTokenType(TokenType.binaryOperator, "=="));
        knownTokens.add(new SimpleTokenType(TokenType.binaryOperator, ">="));
        knownTokens.add(new SimpleTokenType(TokenType.binaryOperator, "<="));
        knownTokens.add(new SimpleTokenType(TokenType.binaryOperator, ">"));
        knownTokens.add(new SimpleTokenType(TokenType.binaryOperator, "<"));
        knownTokens.add(new SimpleTokenType(TokenType.binaryOperator, "!="));
        knownTokens.add(new SimpleTokenType(TokenType.binaryOperator, "not is"));
        knownTokens.add(new SimpleTokenType(TokenType.binaryOperator, "is"));
        knownTokens.add(new SimpleTokenType(TokenType.binaryOperator, "not contains"));
        knownTokens.add(new SimpleTokenType(TokenType.binaryOperator, "contains"));
        knownTokens.add(new SimpleTokenType(TokenType.binaryOperator, "not regex"));
        knownTokens.add(new SimpleTokenType(TokenType.binaryOperator, "regex"));
        knownTokens.add(new SimpleTokenType(TokenType.binaryOperator, "not in"));
        knownTokens.add(new SimpleTokenType(TokenType.binaryOperator, "in"));
        knownTokens.add(new SimpleTokenType(TokenType.binaryOperator, "range"));
        knownTokens.add(new SimpleTokenType(TokenType.binaryOperator, "not range"));

        // unary operators
        knownTokens.add(new SimpleTokenType(TokenType.unaryOperator, "++"));
        knownTokens.add(new SimpleTokenType(TokenType.unaryOperator, "--"));

        // logical operators
        knownTokens.add(new SimpleTokenType(TokenType.logicalOperator, "&&"));
        knownTokens.add(new SimpleTokenType(TokenType.logicalOperator, "||"));
    }

    /**
     * Create the next token
     *
     * @param expression  the input expression
     * @param index       the current index
     * @param filter      defines the accepted token types to be returned (character is always used as fallback)
     * @return the created token, will always return a token
     */
    public SimpleToken nextToken(String expression, int index, TokenType... filter) {
        return doNextToken(expression, index, filter);
    }

    /**
     * Create the next token
     *
     * @param expression  the input expression
     * @param index       the current index
     * @return the created token, will always return a token
     */
    public SimpleToken nextToken(String expression, int index) {
        return doNextToken(expression, index);
    }

    private SimpleToken doNextToken(String expression, int index, TokenType... filters) {

        boolean escapedAllowed = acceptType(TokenType.escapedValue, filters);
        if (escapedAllowed) {
            // is it an escaped value
            if (expression.charAt(index) == '\\' && index < expression.length() - 1) {
                String text = "" + expression.charAt(index + 1);
                // use 2 as length for escaped as we need to jump to the next symbol
                return new SimpleToken(new SimpleTokenType(TokenType.escapedValue, text), index, 2);
            }
        }

        boolean numericAllowed = acceptType(TokenType.numericValue, filters);
        if (numericAllowed) {
            // is it a numeric value
            StringBuilder sb = new StringBuilder();
            boolean digit = true;
            while (digit && index < expression.length()) {
                digit = Character.isDigit(expression.charAt(index));
                if (digit) {
                    char ch = expression.charAt(index);
                    sb.append(ch);
                    index++;
                }
            }
            if (sb.length() > 0) {
                return new SimpleToken(new SimpleTokenType(TokenType.numericValue, sb.toString()), index);
            }
        }

        // it could be any of the known tokens
        String text = expression.substring(index);
        for (SimpleTokenType token : knownTokens) {
            if (acceptType(token.getType(), filters)) {
                if (text.startsWith(token.getValue())) {
                    return new SimpleToken(token, index);
                }
            }
        }

        // fallback and create a character token
        char ch = expression.charAt(index);
        SimpleToken token = new SimpleToken(new SimpleTokenType(TokenType.character, "" + ch), index);
        return token;
    }

    private boolean acceptType(TokenType type, TokenType... filters) {
        if (filters == null || filters.length == 0) {
            return true;
        }
        for (TokenType filter : filters) {
            if (type == filter) {
                return true;
            }
        }
        return false;
    }

}
