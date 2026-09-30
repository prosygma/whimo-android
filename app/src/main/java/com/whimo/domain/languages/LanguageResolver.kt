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

/**
 * Picks the language the app should use among the ones enabled in the admin panel.
 * Same rules as the backend's Accept-Language matching: exact code first, then base code.
 */
object LanguageResolver {

    /**
     * Returns the code in [codes] matching the language [tag] ("fr-CM" matches "fr-CM", then "fr"),
     * or null when none does.
     */
    fun match(tag: String?, codes: List<String>): String? {
        if (tag.isNullOrBlank()) return null

        val normalized = tag.replace('_', '-')
        codes.firstOrNull { it.equals(normalized, ignoreCase = true) }?.let { return it }

        val base = normalized.substringBefore('-')
        return codes.firstOrNull { it.equals(base, ignoreCase = true) }
            ?: codes.firstOrNull { it.substringBefore('-').equals(base, ignoreCase = true) }
    }

    /**
     * Language to use: the [selected] one while it is enabled, else the first enabled device
     * language, else the backend default.
     */
    fun resolve(selected: String?, deviceTags: List<String>, model: LanguagesModel): String {
        val codes = model.codes

        match(selected, codes)?.let { return it }
        deviceTags.firstNotNullOfOrNull { match(it, codes) }?.let { return it }

        return match(model.defaultCode, codes)
            ?: codes.firstOrNull()
            ?: model.defaultCode.ifBlank { AppLanguage.FALLBACK_CODE }
    }
}
