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

import org.apache.camel.test.junit4.LanguageTestSupport;
import org.junit.Test;

/**
 *
 */
public class Simple2Test extends LanguageTestSupport {

    @Override
    protected String getLanguageName() {
        return "simple2";
    }

    @Test
    public void testSimple2Constant() throws Exception {
        assertExpression(exchange, "Hello", "Hello");
        assertExpression(exchange, "'Hello'", "'Hello'");
        assertExpression(exchange, "\"Hello\"", "\"Hello\"");
        assertExpression(exchange, "'Hello \"me\" is good'", "'Hello \"me\" is good'");
        assertExpression(exchange, "\"Hello 'me' is good\"", "\"Hello 'me' is good\"");
        assertExpression(exchange, "Hello \\'me\\' is good", "Hello 'me' is good");
    }

    @Test
    public void testSimple2Body() throws Exception {
        assertExpression(exchange, "${body}", "<hello id='m123'>world!</hello>");
        assertExpression(exchange, "$simple{body}", "<hello id='m123'>world!</hello>");
    }

    @Test
    public void testSimple2ConstantAndBody() throws Exception {
        exchange.getIn().setBody("Camel");
        assertExpression(exchange, "Hi ${body} how are you", "Hi Camel how are you");
        assertExpression(exchange, "'Hi '${body}' how are you'", "'Hi 'Camel' how are you'");
    }

    @Test
    public void testSimple2ConstantAndBodyAndHeader() throws Exception {
        exchange.getIn().setBody("Camel");
        exchange.getIn().setHeader("foo", "Tiger");
        assertExpression(exchange, "Hi ${body} how are ${header.foo}", "Hi Camel how are Tiger");
    }

    @Test
    public void testSimple2EqOperator() throws Exception {
        exchange.getIn().setBody("Camel");
        assertPredicate(exchange, "${body} == 'Tiger'", false);
        assertPredicate(exchange, "${body} == 'Camel'", true);
        assertPredicate(exchange, "${body} == \"Tiger\"", false);
        assertPredicate(exchange, "${body} == \"Camel\"", true);
    }

}