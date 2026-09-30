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
package com.whimo.data.languages.repository

import com.whimo.data.base.common.BaseResult
import com.whimo.data.languages.model.mappers.toDomain
import com.whimo.data.languages.service.LanguagesService
import com.whimo.domain.languages.models.LanguageStringsModel
import com.whimo.domain.languages.models.LanguagesModel
import com.whimo.network.handleResponse
import com.whimo.network.mapResult
import com.whimo.utils.translations.TranslationsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface LanguagesRepository {
    suspend fun getLanguages(): BaseResult<LanguagesModel>
    suspend fun getStrings(code: String): BaseResult<LanguageStringsModel>

    fun getCachedLanguages(): LanguagesModel?
    suspend fun saveLanguages(model: LanguagesModel)
    suspend fun getCachedStringsVersion(code: String): Int?
    suspend fun saveStrings(model: LanguageStringsModel)
    suspend fun keepStrings(codes: List<String>)
}

class LanguagesRepositoryImpl(
    private val service: LanguagesService,
    private val store: TranslationsStore,
) : LanguagesRepository {

    // The list is small and read by every language picker: keep it in memory.
    @Volatile
    private var cachedLanguages: LanguagesModel? = null

    override suspend fun getLanguages(): BaseResult<LanguagesModel> {
        return handleResponse {
            service.getLanguages()
        }.mapResult { it?.toDomain() }
    }

    override suspend fun getStrings(code: String): BaseResult<LanguageStringsModel> {
        return handleResponse {
            service.getStrings(code)
        }.mapResult { it?.toDomain() }
    }

    override fun getCachedLanguages(): LanguagesModel? {
        return cachedLanguages ?: store.readLanguages()?.also { cachedLanguages = it }
    }

    override suspend fun saveLanguages(model: LanguagesModel) {
        cachedLanguages = model
        withContext(Dispatchers.IO) { store.writeLanguages(model) }
    }

    override suspend fun getCachedStringsVersion(code: String): Int? {
        return withContext(Dispatchers.IO) { store.readStrings(code)?.version }
    }

    override suspend fun saveStrings(model: LanguageStringsModel) {
        withContext(Dispatchers.IO) { store.writeStrings(model) }
    }

    override suspend fun keepStrings(codes: List<String>) {
        withContext(Dispatchers.IO) { store.keepStrings(codes) }
    }
}
