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

import com.whimo.data.languages.model.response.LanguageResponse
import com.whimo.data.languages.model.response.LanguageStringsResponse
import com.whimo.data.languages.model.response.LanguagesResponse
import com.whimo.domain.languages.models.AppLanguage
import com.whimo.domain.languages.models.LanguageStringsModel
import com.whimo.domain.languages.models.LanguagesModel

fun LanguagesResponse.toDomain(): LanguagesModel? {
    val data = data ?: return null
    val languages = data.languages.orEmpty().mapNotNull { it.toDomain() }

    return LanguagesModel(
        defaultCode = data.default?.takeIf { it.isNotBlank() }
            ?: languages.firstOrNull()?.code
            ?: AppLanguage.FALLBACK_CODE,
        languages = languages,
    )
}

fun LanguageResponse.toDomain(): AppLanguage? {
    val code = code?.trim()
    if (code.isNullOrEmpty()) return null

    return AppLanguage(
        code = code,
        name = name?.takeIf { it.isNotBlank() } ?: english_name?.takeIf { it.isNotBlank() } ?: code,
        flag = flag.orEmpty(),
        version = version ?: 0,
    )
}

fun LanguageStringsResponse.toDomain(): LanguageStringsModel? {
    val data = data ?: return null
    val code = data.code?.trim()
    if (code.isNullOrEmpty()) return null

    return LanguageStringsModel(
        code = code,
        version = data.version ?: 0,
        strings = data.strings.orEmpty()
            .mapNotNull { (key, value) -> value?.let { key to it } }
            .toMap(),
    )
}
