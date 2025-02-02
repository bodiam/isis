/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.core.metamodel.valuesemantics;

import java.util.Objects;

import org.apache.isis.applib.value.semantics.EncoderDecoder;
import org.apache.isis.applib.value.semantics.OrderRelation;
import org.apache.isis.applib.value.semantics.Renderer;
import org.apache.isis.applib.value.semantics.ValueSemanticsAbstract;
import org.apache.isis.schema.common.v2.ValueType;

import lombok.NonNull;
import lombok.val;

public abstract class XmlValueSemanticsAbstract<T>
extends ValueSemanticsAbstract<T>
implements
    OrderRelation<T, Void>,
    EncoderDecoder<T>,
    Renderer<T> {

    @Override
    public final ValueType getSchemaValueType() {
        return ValueType.STRING;
    }

    // -- ORDER RELATION

    @Override
    public final Void epsilon() {
        return null; // not used
    }

    @Override
    public final int compare(final T a, final T b, final Void epsilon) {
        if(a==null
                || b==null) {
            return Objects.equals(a, b)
                    ? 0
                    : a==null
                        ? -1
                        : 1;
        }
        val _a = toEncodedString(a);
        val _b = toEncodedString(b);
        return _a.compareTo(_b);
    }

    @Override
    public final boolean equals(final T a, final T b, final Void epsilon) {
        if(a==null
                || b==null) {
            return Objects.equals(a, b);
        }
        val _a = toEncodedString(a);
        val _b = toEncodedString(b);
        return _a.equals(_b);
    }

    // -- RENDERER

    @Override
    public String simpleTextPresentation(final Context context, final T value) {
        return render(value, v->renderXml(context, toEncodedString(v)));
    }

    protected String renderXml(final @NonNull Context context, final @NonNull String xml) {
        return xml;
    }

}
