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

import org.apache.camel.Predicate;
import org.apache.camel.test.ExchangeTestSupport;

/**
 *
 */
public class Simple2ParserPredicateTest extends ExchangeTestSupport {

    public void testSimple2Eq() throws Exception {
        exchange.getIn().setBody("foo");

        SimplePredicateParser parser = new SimplePredicateParser("${body} == 'foo'");
        Predicate pre = parser.parsePredicate();

        assertTrue(pre.matches(exchange));
    }

    public void testSimple2EqNumeric() throws Exception {
        exchange.getIn().setBody(123);

        SimplePredicateParser parser = new SimplePredicateParser("${body} == 123");
        Predicate pre = parser.parsePredicate();

        assertTrue("Should match", pre.matches(exchange));
    }

    public void testSimple2EqFunctionFunction() throws Exception {
        exchange.getIn().setBody(122);
        exchange.getIn().setHeader("val", 122);

        SimplePredicateParser parser = new SimplePredicateParser("${body} == ${header.val}");
        Predicate pre = parser.parsePredicate();

        assertTrue("Should match", pre.matches(exchange));
    }

    public void testSimple2EqFunctionNumeric() throws Exception {
        exchange.getIn().setBody(122);

        SimplePredicateParser parser = new SimplePredicateParser("${body} == 122");
        Predicate pre = parser.parsePredicate();

        assertTrue("Should match", pre.matches(exchange));
    }

    public void testSimple2GtFunctionNumeric() throws Exception {
        exchange.getIn().setBody(122);

        SimplePredicateParser parser = new SimplePredicateParser("${body} > 120");
        Predicate pre = parser.parsePredicate();

        assertTrue("Should match", pre.matches(exchange));
    }

    public void testSimple2UnaryInc() throws Exception {
        exchange.getIn().setBody(122);

        SimplePredicateParser parser = new SimplePredicateParser("${body}++ == 123");
        Predicate pre = parser.parsePredicate();

        assertTrue("Should match", pre.matches(exchange));
    }

    public void testSimple2UnaryDec() throws Exception {
        exchange.getIn().setBody(122);

        SimplePredicateParser parser = new SimplePredicateParser("${body}-- == 121");
        Predicate pre = parser.parsePredicate();

        assertTrue("Should match", pre.matches(exchange));
    }

    public void testSimple2EqFunctionBoolean() throws Exception {
        exchange.getIn().setBody("Hello");
        exchange.getIn().setHeader("high", true);

        SimplePredicateParser parser = new SimplePredicateParser("${header.high} == true");
        Predicate pre = parser.parsePredicate();

        assertTrue("Should match", pre.matches(exchange));
    }

    public void testSimple2EqFunctionBooleanSpaces() throws Exception {
        exchange.getIn().setBody("Hello");
        exchange.getIn().setHeader("high", true);

        SimplePredicateParser parser = new SimplePredicateParser("${header.high}   ==     true");
        Predicate pre = parser.parsePredicate();

        assertTrue("Should match", pre.matches(exchange));
    }

    public void testSimple2LogicalAnd() throws Exception {
        exchange.getIn().setBody("Hello");
        exchange.getIn().setHeader("high", true);
        exchange.getIn().setHeader("foo", 123);

        SimplePredicateParser parser = new SimplePredicateParser("${header.high} == true && ${header.foo} == 123");
        Predicate pre = parser.parsePredicate();

        assertTrue("Should match", pre.matches(exchange));
    }

    public void testSimple2LogicalOr() throws Exception {
        exchange.getIn().setBody("Hello");
        exchange.getIn().setHeader("high", true);
        exchange.getIn().setHeader("foo", 123);

        SimplePredicateParser parser = new SimplePredicateParser("${header.high} == false || ${header.foo} == 123");
        Predicate pre = parser.parsePredicate();

        assertTrue("Should match", pre.matches(exchange));
    }

    public void testSimple2LogicalAndAnd() throws Exception {
        exchange.getIn().setBody("Hello");
        exchange.getIn().setHeader("high", true);
        exchange.getIn().setHeader("foo", 123);
        exchange.getIn().setHeader("bar", "beer");

        SimplePredicateParser parser = new SimplePredicateParser("${header.high} == true && ${header.foo} == 123 && ${header.bar} == 'beer'");
        Predicate pre = parser.parsePredicate();

        assertTrue("Should match", pre.matches(exchange));
    }

}
