/*******************************************************************************
 * Copyright (c) 2008, 2026 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.versions.bundle.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;

import org.eclipse.tycho.model.manifest.ManifestAttribute;
import org.junit.jupiter.api.Test;

public class ManifestAttributeTest {

    @Test
    public void shouldCreateWithParameters() {
        // given
        String key = "headername";
        String value = "headervalue";

        // when
        ManifestAttribute attribute = new ManifestAttribute(key, value);

        // then
        assertEquals(value, attribute.getValue());
        assertTrue(attribute.hasName(key));
        assertFalse(attribute.hasName("headernames"));
        assertFalse(attribute.hasName("header"));
    }

    @Test
    public void shouldCreateWithUnixLine() {
        // given
        String key = "headername";
        String value = "headervalue";
        String line = key + ": " + value + "\n";

        // when
        ManifestAttribute attribute = new ManifestAttribute(line);

        // then
        assertEquals(value, attribute.getValue());
        assertTrue(attribute.hasName(key));
    }

    @Test
    public void shouldCreateWithOldMacLine() {
        // given
        String key = "headername";
        String value = "headervalue";
        String line = key + ": " + value + "\r";

        // when
        ManifestAttribute attribute = new ManifestAttribute(line);

        // then
        assertEquals(value, attribute.getValue());
        assertTrue(attribute.hasName(key));
    }

    @Test
    public void shouldCreateWithWindowsLine() {
        // given
        String key = "headername";
        String value = "headervalue";
        String line = key + ": " + value + "\r\n";

        // when
        ManifestAttribute attribute = new ManifestAttribute(line);

        // then
        assertEquals(value, attribute.getValue());
        assertTrue(attribute.hasName(key));
    }

    @Test
    public void shouldCreateWithChoppedLine() {
        // given
        String key = "headername";
        String value = "headervalue";
        String line = key + ": " + value;

        // when
        ManifestAttribute attribute = new ManifestAttribute(line);

        // then
        assertEquals(value, attribute.getValue());
        assertTrue(attribute.hasName(key));
    }

    @Test
    public void shouldAddLine() {
        // given
        String line1 = "headername: headervalue1\n";
        String line2 = " headervalue2\n";
        String line3 = " headervalue3";

        // when
        ManifestAttribute attribute = new ManifestAttribute(line1);
        attribute.add(line2);
        attribute.add(line3);

        // then
        assertEquals("headervalue1headervalue2headervalue3", attribute.getValue());
        assertTrue(attribute.hasName("headername"));
        assertFalse(attribute.hasName("headernames"));
        assertFalse(attribute.hasName("header"));
    }

    @Test
    public void shouldFailOnAddLineWithNewLines() {
        // given
        String line1 = "headername: headervalue1";
        String line2 = " header\n value2\n";

        // when
        ManifestAttribute attribute = new ManifestAttribute(line1);
        assertThrows(IllegalArgumentException.class, () -> attribute.add(line2));
    }

    @Test
    public void shouldWriteToWithUnixNewLine() throws Exception {
        // given
        StringWriter writer = new StringWriter();
        String name = "headername";
        String value = "headervalue";

        // when
        ManifestAttribute attribute = new ManifestAttribute(name, value);
        attribute.writeTo(writer, "\n");

        // then
        assertEquals(name + ": " + value + "\n", writer.toString());
    }

    @Test
    public void shouldWriteToWithWindowsNewLine() throws Exception {
        // given
        StringWriter writer = new StringWriter();
        String name = "headername";
        String value = "headervalue";

        // when
        ManifestAttribute attribute = new ManifestAttribute(name, value);
        attribute.writeTo(writer, "\r\n");

        // then
        assertEquals(name + ": " + value + "\r\n", writer.toString());
    }

    @Test
    public void shouldGetValue() throws Exception {
        // given
        String name = "headername";
        String value = "headervalue";
        String valuelong = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        // when
        ManifestAttribute attribute0 = new ManifestAttribute(name, value);
        ManifestAttribute attribute1 = new ManifestAttribute(name, valuelong);
        ManifestAttribute attribute2 = new ManifestAttribute(name + ": " + valuelong);
        ManifestAttribute attribute3 = new ManifestAttribute(name + ": abcdefghijklmnopq");
        attribute3.add(" rstuvwxyzABCDEFGHIJKLMN");
        attribute3.add(" OPQRSTUVWXYZ0123456789");
        attribute3.add(" abcdefghijklmnop");
        attribute3.add(" qrstuvwxyzABCDE");
        attribute3.add(" FGHIJKLMNOPQRSTUVWXYZ0123");
        attribute3.add(" 456789");

        // then
        assertEquals(value, attribute0.getValue());
        assertEquals(valuelong, attribute1.getValue());
        assertEquals(valuelong, attribute2.getValue());
        assertEquals(valuelong, attribute3.getValue());
    }

    @Test
    public void shouldNotWrapOriginalLines() throws Exception {
        // given
        StringWriter writer = new StringWriter();

        // when
        ManifestAttribute attribute = new ManifestAttribute("headername: abcdefghijklmnopq");
        attribute.add(
                " rstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
        attribute.add(" abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
        attribute.add(" abcdefghijklmnop");
        attribute.add(" qrstuvwxyzABCDE");
        attribute.add(" FGHIJKLMNOPQRSTUVWXYZ0123");
        attribute.add(" 456789");
        attribute.writeTo(writer, "\n");

        // then
        assertEquals(
                """
                        headername: abcdefghijklmnopq
                         rstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789
                         abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789
                         abcdefghijklmnop
                         qrstuvwxyzABCDE
                         FGHIJKLMNOPQRSTUVWXYZ0123
                         456789
                        """,
                writer.toString());
    }
}
