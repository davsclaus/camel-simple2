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

import org.apache.camel.test.ExchangeTestSupport;

/**
 *
 */
public class Simple2ParserExpressionInvalidTest extends ExchangeTestSupport {

    public void testSimple2FunctionInvalid() throws Exception {
        SimpleExpressionParser parser = new SimpleExpressionParser("${body${foo}}");
        try {
            parser.parseExpression();
            fail("Should thrown exception");
        } catch (SimpleIllegalSyntaxException e) {
            assertEquals(6, e.getIndex());
        }
    }

    public void testSimple2UnbalanceFunction() throws Exception {
        SimpleExpressionParser parser = new SimpleExpressionParser("${body is a nice day");
        try {
            parser.parseExpression();
            fail("Should thrown exception");
        } catch (SimpleIllegalSyntaxException e) {
            assertEquals(19, e.getIndex());
        }
    }

    public void testSimple2UnknownFunction() throws Exception {
        SimpleExpressionParser parser = new SimpleExpressionParser("Hello ${foo} how are you?");
        try {
            parser.parseExpression();
            fail("Should thrown exception");
        } catch (SimpleIllegalSyntaxException e) {
            assertEquals(6, e.getIndex());
        }
    }

}
