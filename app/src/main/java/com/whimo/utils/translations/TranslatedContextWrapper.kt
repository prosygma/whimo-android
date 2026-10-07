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

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Context whose resources return the strings downloaded from the backend first.
 * Wrap the base context of every activity with [withTranslations].
 *
 * Never wrap the base context of the Application: the framework casts it to ContextImpl
 * (ActivityThread.handleReceiver), so a broadcast to a manifest receiver would crash the app.
 * The Application overrides getResources() with [TranslatedResourcesHolder] instead.
 */
class TranslatedContextWrapper(base: Context) : ContextWrapper(base) {

    private val holder = TranslatedResourcesHolder(this, TranslationsStore.getInstance(base))

    override fun getResources(): Resources {
        return holder.get(super.getResources())
    }
}

/**
 * Builds and caches the translated resources of a context from its own resources.
 *
 * Below Android 13 it also applies the language chosen in the app, which the system only
 * does by itself from Android 13 (per-app languages).
 */
class TranslatedResourcesHolder(
    private val context: Context,
    private val store: TranslationsStore,
) {

    private var translated: TranslatedResources? = null
    private var localized: LocalizedResources? = null

    @Synchronized
    fun get(resources: Resources): Resources {
        val source = localizedResources(resources)
        translated?.let {
            // Rebuilt when the configuration changes (the application context is never recreated).
            if (it.base === source && it.configuration == source.configuration) return it
        }
        return TranslatedResources(source, store).also { translated = it }
    }

    private fun localizedResources(resources: Resources): Resources {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return resources

        val code = store.selectedLanguageCode ?: return resources
        val locale = Locale.forLanguageTag(code)
        if (resources.configuration.locales[0] == locale) return resources

        localized?.let {
            if (it.code == code && it.source == resources.configuration) return it.resources
        }

        val configuration = Configuration(resources.configuration).apply {
            setLocales(LocaleList(locale))
        }
        return context.createConfigurationContext(configuration).resources.also {
            localized = LocalizedResources(code, Configuration(resources.configuration), it)
        }
    }

    private class LocalizedResources(
        val code: String,
        val source: Configuration,
        val resources: Resources,
    )
}

fun Context.withTranslations(): Context {
    return this as? TranslatedContextWrapper ?: TranslatedContextWrapper(this)
}
