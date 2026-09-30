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
package com.whimo.utils

import android.app.Activity
import android.app.LocaleConfig
import android.app.LocaleManager
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.whimo.utils.translations.TranslationsStore

interface AppLocaleManager {
    fun changeLanguage(context: Context, languageCode: String)

    /** Language actually in use: the one chosen for the app, else the device one. */
    fun getLanguageCode(context: Context): String

    /** Language chosen in the app (or in the system per-app language settings), null when following the device. */
    fun getSelectedLanguageCode(context: Context): String?

    /** Device languages, most preferred first, whatever the app language is. */
    fun getDeviceLanguageTags(): List<String>

    /** Lets the system per-app language settings (Android 14+) offer the languages enabled in the admin panel. */
    fun setSupportedLanguages(context: Context, languageCodes: List<String>)
}

class AppLocaleManagerImpl : AppLocaleManager {

    override fun changeLanguage(context: Context, languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(languageCode)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
            // The activities are not AppCompat ones: the context wrappers apply the stored language.
            TranslationsStore.getInstance(context).selectedLanguageCode = languageCode
            (context as? Activity)?.recreate()
        }
    }

    override fun getLanguageCode(context: Context): String {
        return getSelectedLanguageCode(context)
            ?: context.resources.configuration.locales[0]?.toLanguageTag()?.takeIf { it != UNDEFINED_LANGUAGE }
            ?: DEFAULT_LANGUAGE
    }

    override fun getSelectedLanguageCode(context: Context): String? {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                ?.applicationLocales
                ?.get(0)
        } else {
            AppCompatDelegate.getApplicationLocales().get(0)
        }
        return locale?.toLanguageTag()
            ?: if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                TranslationsStore.getInstance(context).selectedLanguageCode
            } else {
                null
            }
    }

    override fun getDeviceLanguageTags(): List<String> {
        val locales = Resources.getSystem().configuration.locales
        return (0 until locales.size()).map { locales[it].toLanguageTag() }
    }

    override fun setSupportedLanguages(context: Context, languageCodes: List<String>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || languageCodes.isEmpty()) return
        try {
            context.getSystemService(LocaleManager::class.java)?.overrideLocaleConfig =
                LocaleConfig(LocaleList.forLanguageTags(languageCodes.joinToString(",")))
        } catch (e: Exception) {
            Log.e("AppLocaleManager", "Could not update the supported languages", e)
        }
    }

    companion object {
        private const val DEFAULT_LANGUAGE = "en"
        private const val UNDEFINED_LANGUAGE = "und"
    }
}
