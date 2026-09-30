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

import java.util.Locale

/**
 * Lookup and formatting rules for strings downloaded from the backend.
 */
object StringOverrides {

    // Resources of the app itself (as opposed to the framework) have ids starting with 0x7f.
    private const val APP_PACKAGE_ID = 0x7f

    fun isAppResource(id: Int): Boolean {
        return id ushr 24 == APP_PACKAGE_ID
    }

    /**
     * Downloaded text for the string resource [entryName], or null to use the bundled one.
     * An empty value is treated as missing.
     */
    fun find(overrides: Map<String, String>?, entryName: String?): String? {
        if (overrides == null || entryName == null) return null
        return overrides[entryName]?.takeIf { it.isNotEmpty() }
    }

    /**
     * Formats a downloaded [template] like Resources.getString(id, args) does.
     * Returns null when its placeholders do not match [args], so that the caller can use
     * the bundled string instead of crashing on a translation mistake.
     */
    fun format(locale: Locale, template: String, args: Array<out Any?>): String? {
        return try {
            String.format(locale, template, *args)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
