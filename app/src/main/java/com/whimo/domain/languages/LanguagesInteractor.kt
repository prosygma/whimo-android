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

import android.content.Context
import com.whimo.data.base.common.BaseResult
import com.whimo.data.base.common.onSuccess
import com.whimo.data.languages.repository.LanguagesRepository
import com.whimo.domain.languages.models.LanguagesModel
import com.whimo.utils.AppLocaleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface LanguagesInteractor {
    /** Enabled languages from the last successful fetch, else the bundled ones. */
    fun getLanguages(): LanguagesModel

    /** Code of the language in use, as listed by [getLanguages] when it is listed. */
    fun getCurrentLanguageCode(context: Context): String

    /** Fetches and caches the enabled languages. */
    suspend fun refreshLanguages(context: Context): BaseResult<LanguagesModel>

    /** Downloads the strings of [code] when the cached ones are older than the listed version. */
    suspend fun updateStrings(code: String)

    /** Downloads the strings of [code], then switches the app to it. */
    suspend fun changeLanguage(context: Context, code: String)

    /**
     * Run at app start: refreshes the languages, moves the app off a language that is not
     * enabled any more and updates the strings of the language in use.
     */
    suspend fun syncLanguages(context: Context)
}

class LanguagesInteractorImpl(
    private val repository: LanguagesRepository,
    private val appLocaleManager: AppLocaleManager,
) : LanguagesInteractor {

    override fun getLanguages(): LanguagesModel {
        return repository.getCachedLanguages() ?: LanguagesModel.BUNDLED
    }

    override fun getCurrentLanguageCode(context: Context): String {
        val code = appLocaleManager.getLanguageCode(context)
        return LanguageResolver.match(code, getLanguages().codes) ?: code
    }

    override suspend fun refreshLanguages(context: Context): BaseResult<LanguagesModel> {
        return repository.getLanguages()
            .onSuccess { model ->
                if (model != null) {
                    repository.saveLanguages(model)
                    appLocaleManager.setSupportedLanguages(context, model.codes)
                }
            }
    }

    override suspend fun updateStrings(code: String) {
        val language = getLanguages().find(code) ?: return
        val cachedVersion = repository.getCachedStringsVersion(code)
        if (cachedVersion != null && cachedVersion >= language.version) return

        repository.getStrings(code)
            .onSuccess { model ->
                if (model != null) repository.saveStrings(model)
            }
    }

    override suspend fun changeLanguage(context: Context, code: String) {
        updateStrings(code)
        withContext(Dispatchers.Main) {
            appLocaleManager.changeLanguage(context, code)
        }
    }

    override suspend fun syncLanguages(context: Context) {
        val result = refreshLanguages(context)
        // Without a list from the backend (never loaded), keep whatever language is in use.
        val model = repository.getCachedLanguages() ?: return

        if (result is BaseResult.Success) {
            repository.keepStrings(model.codes)
        }

        val selected = appLocaleManager.getSelectedLanguageCode(context)
        val deviceTags = appLocaleManager.getDeviceLanguageTags()
        val code = LanguageResolver.resolve(selected, deviceTags, model)

        updateStrings(code)

        val inUse = selected ?: deviceTags.firstOrNull()
        if (LanguageResolver.match(inUse, listOf(code)) == null) {
            withContext(Dispatchers.Main) {
                appLocaleManager.changeLanguage(context, code)
            }
        }
    }
}
