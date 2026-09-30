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

import android.content.res.Resources
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Resources returning the strings downloaded from the backend for the current language
 * before the bundled ones.
 *
 * Covers getString/getText, which is what Compose stringResource() and Context.getString()
 * use. Plurals, string arrays and strings referenced from XML are not overridden.
 */
@Suppress("DEPRECATION")
class TranslatedResources(
    val base: Resources,
    private val store: TranslationsStore,
) : Resources(base.assets, base.displayMetrics, base.configuration) {

    private val entryNames = ConcurrentHashMap<Int, String>()

    override fun getText(id: Int): CharSequence {
        return findOverride(id) ?: base.getText(id)
    }

    override fun getText(id: Int, def: CharSequence?): CharSequence? {
        return findOverride(id) ?: base.getText(id, def)
    }

    override fun getString(id: Int): String {
        return findOverride(id) ?: base.getString(id)
    }

    override fun getString(id: Int, vararg formatArgs: Any?): String {
        val template = findOverride(id) ?: return base.getString(id, *formatArgs)
        return StringOverrides.format(locale(), template, formatArgs) ?: base.getString(id, *formatArgs)
    }

    private fun findOverride(id: Int): String? {
        if (!StringOverrides.isAppResource(id)) return null
        val overrides = store.overridesFor(locale()) ?: return null
        return StringOverrides.find(overrides, entryName(id))
    }

    private fun entryName(id: Int): String? {
        entryNames[id]?.let { return it }
        return try {
            base.getResourceEntryName(id).also { entryNames[id] = it }
        } catch (e: NotFoundException) {
            null
        }
    }

    private fun locale(): Locale {
        return base.configuration.locales[0] ?: Locale.getDefault()
    }
}
