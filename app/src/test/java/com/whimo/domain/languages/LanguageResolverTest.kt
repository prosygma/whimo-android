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
package com.whimo.domain.languages

import com.whimo.domain.languages.models.AppLanguage
import com.whimo.domain.languages.models.LanguagesModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguageResolverTest {

    private val model = LanguagesModel(
        defaultCode = "fr",
        languages = listOf(
            AppLanguage(code = "en", name = "English"),
            AppLanguage(code = "fr", name = "Français"),
            AppLanguage(code = "hi", name = "हिन्दी", flag = "🇮🇳", version = 2),
        ),
    )

    @Test
    fun `match prefers the exact code then the base code`() {
        val codes = listOf("pt", "pt-BR", "fr")

        assertEquals("pt-BR", LanguageResolver.match("pt-BR", codes))
        assertEquals("pt", LanguageResolver.match("pt-PT", codes))
        assertEquals("fr", LanguageResolver.match("fr-CM", codes))
        assertEquals("fr", LanguageResolver.match("fr_CM", codes))
        assertEquals("fr", LanguageResolver.match("FR", codes))
    }

    @Test
    fun `match falls back to a regional code with the same base`() {
        assertEquals("pt-BR", LanguageResolver.match("pt", listOf("pt-BR")))
    }

    @Test
    fun `match returns null for unknown or empty tags`() {
        assertNull(LanguageResolver.match("de", listOf("en", "fr")))
        assertNull(LanguageResolver.match(null, listOf("en")))
        assertNull(LanguageResolver.match("", listOf("en")))
    }

    @Test
    fun `resolve keeps the selected language while it is enabled`() {
        assertEquals("hi", LanguageResolver.resolve("hi", listOf("en-US"), model))
    }

    @Test
    fun `resolve uses the device language when the selected one is disabled`() {
        assertEquals("en", LanguageResolver.resolve("es", listOf("es-ES", "en-GB"), model))
    }

    @Test
    fun `resolve uses the device language when nothing is selected`() {
        assertEquals("fr", LanguageResolver.resolve(null, listOf("fr-CM"), model))
    }

    @Test
    fun `resolve falls back to the backend default`() {
        assertEquals("fr", LanguageResolver.resolve("es", listOf("de-DE"), model))
    }

    @Test
    fun `resolve falls back to the first language when the default is not enabled`() {
        val model = model.copy(defaultCode = "es")

        assertEquals("en", LanguageResolver.resolve(null, listOf("de"), model))
    }

    @Test
    fun `resolve returns the default when no language is enabled`() {
        val model = LanguagesModel(defaultCode = "fr", languages = emptyList())

        assertEquals("fr", LanguageResolver.resolve("en", listOf("en"), model))
    }
}
