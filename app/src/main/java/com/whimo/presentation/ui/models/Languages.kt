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
package com.whimo.presentation.ui.models

import com.whimo.R
import com.whimo.domain.languages.models.AppLanguage

/**
 * Name shown in the language pickers: the app's own name for the bundled languages,
 * else the native name set in the admin panel.
 */
val AppLanguage.label: String
    get() = AppLanguage.bundled(code)?.name ?: name

/**
 * Flag drawable shipped with the app, null for languages added in the admin panel.
 */
val AppLanguage.iconRes: Int?
    get() = when (code) {
        "en" -> R.drawable.ic_en
        "fr" -> R.drawable.ic_fr
        "es" -> R.drawable.ic_sp
        else -> null
    }

fun List<AppLanguage>.labelFor(code: String): String {
    return find { it.code == code }?.label ?: AppLanguage.bundled(code)?.name ?: code
}
