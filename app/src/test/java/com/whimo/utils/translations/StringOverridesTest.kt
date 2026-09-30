/*
 * Copyright (c) 2025 EFI (https://efi.int/)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.whimo.utils.translations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class StringOverridesTest {

    private val overrides = mapOf(
        "login_button" to "लॉग इन करें",
        "empty" to "",
        "greeting" to "Bonjour %1\$s, vous avez %2\$d messages",
        "simple" to "%s kg",
        "decimal" to "%.2f kg",
    )

    @Test
    fun `find returns the downloaded text for the entry name`() {
        assertEquals("लॉग इन करें", StringOverrides.find(overrides, "login_button"))
    }

    @Test
    fun `find returns null when the string was not downloaded or is empty`() {
        assertNull(StringOverrides.find(overrides, "password"))
        assertNull(StringOverrides.find(overrides, "empty"))
        assertNull(StringOverrides.find(overrides, null))
        assertNull(StringOverrides.find(null, "login_button"))
    }

    @Test
    fun `format fills positional and simple placeholders`() {
        assertEquals(
            "Bonjour Ana, vous avez 3 messages",
            StringOverrides.format(Locale.FRENCH, overrides.getValue("greeting"), arrayOf("Ana", 3)),
        )
        assertEquals("12 kg", StringOverrides.format(Locale.ENGLISH, overrides.getValue("simple"), arrayOf("12")))
    }

    @Test
    fun `format uses the locale of the language`() {
        assertEquals("1,50 kg", StringOverrides.format(Locale.FRENCH, overrides.getValue("decimal"), arrayOf(1.5)))
    }

    @Test
    fun `format returns null when the placeholders do not match the arguments`() {
        assertNull(StringOverrides.format(Locale.ENGLISH, "%1\$d items", arrayOf("not a number")))
        assertNull(StringOverrides.format(Locale.ENGLISH, "%1\$s and %2\$s", arrayOf("one")))
    }

    @Test
    fun `only app resources are overridden`() {
        assertTrue(StringOverrides.isAppResource(0x7f120001))
        assertFalse(StringOverrides.isAppResource(0x01040000))
    }
}
