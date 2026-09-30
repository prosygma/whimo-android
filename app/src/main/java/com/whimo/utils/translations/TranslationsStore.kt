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
import android.util.Log
import com.google.gson.Gson
import com.whimo.domain.languages.LanguageResolver
import com.whimo.domain.languages.models.LanguageStringsModel
import com.whimo.domain.languages.models.LanguagesModel
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Disk cache of the language list and of the strings downloaded for each language.
 *
 * It lives outside Koin because the context wrappers read it before Koin starts
 * (Application.attachBaseContext). Files are kept in their own directory and preferences
 * so that logging out (which clears the app preferences) keeps them.
 */
class TranslationsStore private constructor(context: Context) {

    private val directory = File(context.filesDir, DIRECTORY_NAME)
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val strings = ConcurrentHashMap<String, LanguageStringsModel>()

    @Volatile
    private var storedCodes: List<String>? = null

    /**
     * Code chosen in the app. Only needed below Android 13, where per-app languages are not
     * handled by the system for the activities used here.
     */
    var selectedLanguageCode: String?
        get() = preferences.getString(SELECTED_LANGUAGE_KEY, null)
        set(value) {
            preferences.edit().putString(SELECTED_LANGUAGE_KEY, value).apply()
        }

    fun readLanguages(): LanguagesModel? {
        val model = read(File(directory, LANGUAGES_FILE_NAME), LanguagesModel::class.java) ?: return null
        // Gson ignores Kotlin nullability: drop files written by an incompatible version.
        @Suppress("SENSELESS_COMPARISON")
        if (model.defaultCode == null || model.languages == null) return null
        return model
    }

    fun writeLanguages(model: LanguagesModel) {
        write(File(directory, LANGUAGES_FILE_NAME), model)
    }

    fun readStrings(code: String): LanguageStringsModel? {
        strings[code]?.let { return it }

        val file = stringsFile(code) ?: return null
        val model = read(file, LanguageStringsModel::class.java)
        @Suppress("SENSELESS_COMPARISON")
        if (model == null || model.strings == null) {
            // Unreadable: forget it rather than reading it again for every string.
            file.delete()
            storedCodes = null
            return null
        }

        strings[code] = model
        return model
    }

    fun writeStrings(model: LanguageStringsModel) {
        val file = stringsFile(model.code) ?: return
        write(file, model)
        strings[model.code] = model
        storedCodes = null
    }

    /**
     * Deletes the strings of languages that are no longer enabled.
     */
    fun keepStrings(codes: List<String>) {
        storedCodes().filter { it !in codes }.forEach { code ->
            stringsFile(code)?.delete()
            strings.remove(code)
        }
        storedCodes = null
    }

    /**
     * Strings overriding the bundled ones for the language [locale] shows, or null when none
     * were downloaded for it.
     */
    fun overridesFor(locale: Locale): Map<String, String>? {
        val code = LanguageResolver.match(locale.toLanguageTag(), storedCodes()) ?: return null
        return readStrings(code)?.strings?.takeIf { it.isNotEmpty() }
    }

    private fun storedCodes(): List<String> {
        storedCodes?.let { return it }

        val codes = directory.listFiles().orEmpty()
            .map { it.name }
            .filter { it.startsWith(STRINGS_FILE_PREFIX) && it.endsWith(JSON_EXTENSION) }
            .map { it.removePrefix(STRINGS_FILE_PREFIX).removeSuffix(JSON_EXTENSION) }
        storedCodes = codes
        return codes
    }

    private fun stringsFile(code: String): File? {
        if (!CODE_PATTERN.matches(code)) return null
        return File(directory, "$STRINGS_FILE_PREFIX$code$JSON_EXTENSION")
    }

    private fun <T> read(file: File, type: Class<T>): T? {
        if (!file.exists()) return null
        return try {
            file.reader().use { gson.fromJson(it, type) }
        } catch (e: Exception) {
            Log.e(TAG, "Could not read ${file.name}", e)
            null
        }
    }

    private fun write(file: File, value: Any) {
        try {
            directory.mkdirs()
            // Write then rename, so that a crash never leaves a half-written file behind.
            val temporary = File(directory, "${file.name}.tmp")
            temporary.writeText(gson.toJson(value))
            if (!temporary.renameTo(file)) {
                file.delete()
                temporary.renameTo(file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not write ${file.name}", e)
        }
    }

    companion object {
        private const val TAG = "TranslationsStore"
        private const val DIRECTORY_NAME = "languages"
        private const val PREFERENCES_NAME = "whimo_languages"
        private const val SELECTED_LANGUAGE_KEY = "selected_language"
        private const val LANGUAGES_FILE_NAME = "languages.json"
        private const val STRINGS_FILE_PREFIX = "strings_"
        private const val JSON_EXTENSION = ".json"
        private val CODE_PATTERN = Regex("[A-Za-z0-9_-]{1,35}")

        @Volatile
        private var instance: TranslationsStore? = null

        fun getInstance(context: Context): TranslationsStore {
            return instance ?: synchronized(this) {
                // The application context is still null inside Application.attachBaseContext.
                instance ?: TranslationsStore(context.applicationContext ?: context).also { instance = it }
            }
        }
    }
}
