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
package org.apache.isis.core.metamodel.facets.value;

import java.time.LocalDateTime;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.isis.applib.exceptions.recoverable.TextEntryParseException;
import org.apache.isis.applib.locale.UserLocale;
import org.apache.isis.applib.services.iactnlayer.InteractionContext;
import org.apache.isis.applib.value.semantics.ValueSemanticsAbstract;
import org.apache.isis.applib.value.semantics.ValueSemanticsProvider.Context;
import org.apache.isis.core.metamodel.valuesemantics.temporal.LocalDateTimeValueSemantics;
import org.apache.isis.core.metamodel.valuesemantics.temporal.legacy.JavaUtilDateValueSemantics;

import lombok.val;

class JavaUtilDateValueSemanticsProviderTest
extends ValueSemanticsProviderAbstractTestCase {

    @SuppressWarnings("deprecation")
    private final java.util.Date date = new java.util.Date(2013-1900, 03-1, 13, 17, 59, 03);
    private JavaUtilDateValueSemantics valueSemantics;

    @BeforeEach
    public void setUpObjects() throws Exception {

        ValueSemanticsAbstract<LocalDateTime> delegate =
                new LocalDateTimeValueSemantics();

        setSemantics(valueSemantics = new JavaUtilDateValueSemantics() {
            @Override
            public ValueSemanticsAbstract<LocalDateTime> getDelegate() {
                return delegate;
            }
        });
    }

    @Test
    public void testInvalidParse() throws Exception {
        try {
            valueSemantics.parseTextRepresentation(null, "invalid entry");
            fail();
        } catch (final TextEntryParseException expected) {
        }
    }

    /**
     * Something rather bizarre here, that the epoch formats as 01:00 rather
     * than 00:00. It's obviously because of some sort of timezone issue, but I
     * don't know where that dependency is coming from.
     */
    @Test
    public void testRendering() {
        val _context = Context.of(null, InteractionContext.builder().locale(UserLocale.valueOf(Locale.ENGLISH)).build());
        assertEquals("Mar 13, 2013, 5:59:03 PM", valueSemantics.simpleTextPresentation(_context , date));
    }

    //FIXME[ISIS-2882] support omitted parts on input
    @Test @Disabled
    public void testParseNoMinutes() throws Exception {
        val _context = Context.of(null, InteractionContext.builder().locale(UserLocale.valueOf(Locale.ENGLISH)).build());
        val parsedDate = valueSemantics.parseTextRepresentation(_context, "2013-03-13 17");
        assertEquals(date.getTime() - 3540_000L - 3000L, parsedDate.getTime());
    }

    //FIXME[ISIS-2882] support omitted parts on input
    @Test @Disabled
    public void testParseNoSeconds() throws Exception {
        val _context = Context.of(null, InteractionContext.builder().locale(UserLocale.valueOf(Locale.ENGLISH)).build());
        val parsedDate = valueSemantics.parseTextRepresentation(_context, "2013-03-13 17:59");
        assertEquals(date.getTime() - 3000L, parsedDate.getTime());
    }

    @Test
    public void testParseSeconds() throws Exception {
        val _context = Context.of(null, InteractionContext.builder().locale(UserLocale.valueOf(Locale.ENGLISH)).build());
        val parsedDate = valueSemantics.parseTextRepresentation(_context, "2013-03-13 17:59:03");
        assertEquals(date.getTime(), parsedDate.getTime());
    }

    //FIXME[ISIS-2882] support omitted parts on input
    /**
     * @see https://stackoverflow.com/questions/30103167/jsr-310-parsing-seconds-fraction-with-variable-length
     */
    @Test @Disabled("cannot find a format pattern that can handle both millis and nanos")
    public void testParseMillis() throws Exception {
        val _context = Context.of(null, InteractionContext.builder().locale(UserLocale.valueOf(Locale.ENGLISH)).build());
        val parsedDate = valueSemantics.parseTextRepresentation(_context, "2013-03-13 17:59:03.123");
        assertEquals(date.getTime() + 123L, parsedDate.getTime());
    }

    //FIXME[ISIS-2882] support omitted parts on input
    @Test @Disabled
    public void testParseNanos() throws Exception {
        val _context = Context.of(null, InteractionContext.builder().locale(UserLocale.valueOf(Locale.ENGLISH)).build());
        val parsedDate = valueSemantics.parseTextRepresentation(_context, "2013-03-13 17:59:03.123456789");
        assertEquals(date.getTime() + 123L, parsedDate.getTime());
    }

}
