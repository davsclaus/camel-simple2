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

import org.apache.camel.Expression;
import org.apache.camel.language.simple.SimpleToken;
import org.apache.camel.language.simple.UnaryOperatorType;
import org.apache.camel.util.ObjectHelper;

/**
 * Represents an unary operator in the AST
 */
public class UnaryOperator extends BaseSimpleNode {

    private UnaryOperatorType operator;
    private SimpleNode node;

    public UnaryOperator(SimpleToken symbol) {
        super(symbol);
    }

    @Override
    public String toString() {
        if (node != null) {
            return node + symbol.getText();
        } else {
            return symbol.getText();
        }
    }

    /**
     * Accepts the left node to this operator
     *
     * @param left  the left node to accept
     */
    public void acceptLeft(SimpleNode left) {
        this.node = left;
    }

    @Override
    public Expression createExpression(String expression) {
        ObjectHelper.notNull(node, "left node", this);
        return null;
    }
}
