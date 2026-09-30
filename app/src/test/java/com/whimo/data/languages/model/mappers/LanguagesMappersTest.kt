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
package com.whimo.data.languages.model.mappers

import com.google.gson.Gson
import com.whimo.data.languages.model.response.LanguageStringsResponse
import com.whimo.data.languages.model.response.LanguagesResponse
import com.whimo.domain.languages.models.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguagesMappersTest {

    private val gson = Gson()

    @Test
    fun `languages response is parsed in picker order`() {
        val json = """
            {"success":true,"data":{"default":"fr","languages":[
              {"code":"en","name":"English","english_name":"English","flag":"","version":1},
              {"code":"hi","name":"हिन्दी","english_name":"Hindi","flag":"🇮🇳","version":2}
            ]}}
        """.trimIndent()

        val model = gson.fromJson(json, LanguagesResponse::class.java).toDomain()!!

        assertEquals("fr", model.defaultCode)
        assertEquals(
            listOf(
                AppLanguage(code = "en", name = "English", flag = "", version = 1),
                AppLanguage(code = "hi", name = "हिन्दी", flag = "🇮🇳", version = 2),
            ),
            model.languages,
        )
    }

    @Test
    fun `languages without code are dropped and missing fields get defaults`() {
        val json = """
            {"success":true,"data":{"languages":[
              {"code":"","name":"Empty"},
              {"code":"de","english_name":"German"},
              {"code":"it"}
            ]}}
        """.trimIndent()

        val model = gson.fromJson(json, LanguagesResponse::class.java).toDomain()!!

        assertEquals("de", model.defaultCode)
        assertEquals(
            listOf(
                AppLanguage(code = "de", name = "German", flag = "", version = 0),
                AppLanguage(code = "it", name = "it", flag = "", version = 0),
            ),
            model.languages,
        )
    }

    @Test
    fun `languages response without data maps to null`() {
        val json = """{"success":false}"""

        assertNull(gson.fromJson(json, LanguagesResponse::class.java).toDomain())
    }

    @Test
    fun `strings response keeps placeholders, newlines and apostrophes`() {
        val json = """
            {"success":true,"data":{"code":"hi","version":2,"strings":{
              "email":"ईमेल",
              "welcome":"Hello %1${'$'}s,\nyou have %2${'$'}d items",
              "quote":"l'origine",
              "missing":null
            }}}
        """.trimIndent()

        val model = gson.fromJson(json, LanguageStringsResponse::class.java).toDomain()!!

        assertEquals("hi", model.code)
        assertEquals(2, model.version)
        assertEquals(
            mapOf(
                "email" to "ईमेल",
                "welcome" to "Hello %1\$s,\nyou have %2\$d items",
                "quote" to "l'origine",
            ),
            model.strings,
        )
    }

    @Test
    fun `empty strings response maps to an empty map`() {
        val json = """{"success":true,"data":{"code":"en","version":1,"strings":{}}}"""

        val model = gson.fromJson(json, LanguageStringsResponse::class.java).toDomain()!!

        assertEquals(emptyMap<String, String>(), model.strings)
    }
}
