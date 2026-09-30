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
package com.whimo.domain.languages.models

/**
 * A language the app can be displayed in, as managed in the admin panel.
 *
 * [name] is the native name, [flag] an optional emoji ("" when not set) and [version]
 * grows each time an administrator uploads new strings for the language.
 */
data class AppLanguage(
    val code: String,
    val name: String,
    val flag: String = "",
    val version: Int = 0,
) {
    companion object {
        const val FALLBACK_CODE = "en"

        // Languages shipped with the app, used until the backend list has been loaded once.
        val BUNDLED = listOf(
            AppLanguage(code = "en", name = "English"),
            AppLanguage(code = "fr", name = "French"),
            AppLanguage(code = "es", name = "Spanish"),
        )

        fun bundled(code: String?): AppLanguage? {
            return BUNDLED.find { it.code == code }
        }
    }
}

/**
 * The enabled languages, in picker order, and the one the backend uses by default.
 */
data class LanguagesModel(
    val defaultCode: String,
    val languages: List<AppLanguage>,
) {
    val codes: List<String>
        get() = languages.map { it.code }

    fun find(code: String?): AppLanguage? {
        return languages.find { it.code == code }
    }

    companion object {
        val BUNDLED = LanguagesModel(
            defaultCode = AppLanguage.FALLBACK_CODE,
            languages = AppLanguage.BUNDLED,
        )
    }
}

/**
 * Strings uploaded for a language, keyed by Android string resource entry name.
 */
data class LanguageStringsModel(
    val code: String,
    val version: Int,
    val strings: Map<String, String>,
)
