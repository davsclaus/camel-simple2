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

import org.apache.camel.Expression;
import org.apache.camel.ExpressionIllegalSyntaxException;
import org.apache.camel.test.ExchangeTestSupport;

/**
 *
 */
public class Simple2ParserExpressionTest extends ExchangeTestSupport {

    public void testSimple2ParserEol() throws Exception {
        SimpleExpressionParser parser = new SimpleExpressionParser("Hello");
        Expression exp = parser.parseExpression();

        assertEquals("Hello", exp.evaluate(exchange, String.class));
    }

    public void testSimple2SingleQuote() throws Exception {
        SimpleExpressionParser parser = new SimpleExpressionParser("'Hello'");
        Expression exp = parser.parseExpression();

        assertEquals("'Hello'", exp.evaluate(exchange, String.class));
    }

    public void testSimple2SingleQuoteWithFunction() throws Exception {
        exchange.getIn().setBody("World");
        SimpleExpressionParser parser = new SimpleExpressionParser("'Hello ${body} how are you?'");
        Expression exp = parser.parseExpression();

        assertEquals("'Hello World how are you?'", exp.evaluate(exchange, String.class));
    }

    public void testSimple2SingleQuoteWithFunctionBodyAs() throws Exception {
        exchange.getIn().setBody("World");
        SimpleExpressionParser parser = new SimpleExpressionParser("'Hello ${bodyAs(String)} how are you?'");
        Expression exp = parser.parseExpression();

        assertEquals("'Hello World how are you?'", exp.evaluate(exchange, String.class));
    }

    public void testSimple2SingleQuoteEol() throws Exception {
        SimpleExpressionParser parser = new SimpleExpressionParser("'Hello' World");
        Expression exp = parser.parseExpression();

        assertEquals("'Hello' World", exp.evaluate(exchange, String.class));
    }

    public void testSimple2Function() throws Exception {
        exchange.getIn().setBody("World");
        SimpleExpressionParser parser = new SimpleExpressionParser("${body}");
        Expression exp = parser.parseExpression();

        assertEquals("World", exp.evaluate(exchange, String.class));
   }

    public void testSimple2FunctionInvalid() throws Exception {
        SimpleExpressionParser parser = new SimpleExpressionParser("${body${foo}}");
        try {
            parser.parseExpression();
            fail("Should thrown exception");
        } catch (ExpressionIllegalSyntaxException e) {
            // expected
            System.out.println(e.getMessage());
        }
    }

    public void testSimple2SingleQuoteWithEscape() throws Exception {
        SimpleExpressionParser parser = new SimpleExpressionParser("Pay 200\\$ today");
        Expression exp = parser.parseExpression();

        assertEquals("Pay 200$ today", exp.evaluate(exchange, String.class));
    }

    public void testSimple2SingleQuoteWithEscapeEnd() throws Exception {
        SimpleExpressionParser parser = new SimpleExpressionParser("Pay 200\\$");
        Expression exp = parser.parseExpression();

        assertEquals("Pay 200$", exp.evaluate(exchange, String.class));
    }

    public void testSimple2UnbalanceFunction() throws Exception {
        SimpleExpressionParser parser = new SimpleExpressionParser("${body is a nice day");
        try {
            parser.parseExpression();
            fail("Should thrown exception");
        } catch (ExpressionIllegalSyntaxException e) {
            // expected
            System.out.println(e.getMessage());
        }
    }

    public void testSimple2UnaryInc() throws Exception {
        exchange.getIn().setBody("122");
        SimpleExpressionParser parser = new SimpleExpressionParser("${body}++");
        Expression exp = parser.parseExpression();

        assertEquals("123", exp.evaluate(exchange, String.class));
    }

    public void testSimple2UnaryDec() throws Exception {
        exchange.getIn().setBody("122");
        SimpleExpressionParser parser = new SimpleExpressionParser("${body}--");
        Expression exp = parser.parseExpression();

        assertEquals("121", exp.evaluate(exchange, String.class));
    }

}
