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
import com.whimo.data.languages.repository.LanguagesRepository
import com.whimo.domain.languages.models.AppLanguage
import com.whimo.domain.languages.models.LanguageStringsModel
import com.whimo.domain.languages.models.LanguagesModel
import com.whimo.utils.AppLocaleManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class LanguagesInteractorImplTest {

    private lateinit var repository: LanguagesRepository
    private lateinit var appLocaleManager: AppLocaleManager
    private lateinit var interactor: LanguagesInteractorImpl
    private val context = mockk<Context>(relaxed = true)

    private val model = LanguagesModel(
        defaultCode = "fr",
        languages = listOf(
            AppLanguage(code = "en", name = "English", version = 1),
            AppLanguage(code = "fr", name = "Français", version = 1),
            AppLanguage(code = "hi", name = "हिन्दी", version = 2),
        ),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = mockk(relaxUnitFun = true)
        appLocaleManager = mockk(relaxUnitFun = true)
        interactor = LanguagesInteractorImpl(repository, appLocaleManager)

        coEvery { repository.getLanguages() } returns BaseResult.Success(model)
        every { repository.getCachedLanguages() } returns model
        coEvery { repository.getCachedStringsVersion(any()) } returns 1
        coEvery { repository.getStrings(any()) } returns BaseResult.Error(IOException())
        every { appLocaleManager.getDeviceLanguageTags() } returns listOf("de-DE")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getLanguages falls back to the bundled languages`() {
        every { repository.getCachedLanguages() } returns null

        assertEquals(LanguagesModel.BUNDLED, interactor.getLanguages())
    }

    @Test
    fun `getCurrentLanguageCode maps the locale in use to an enabled code`() {
        every { appLocaleManager.getLanguageCode(context) } returns "fr-CM"

        assertEquals("fr", interactor.getCurrentLanguageCode(context))
    }

    @Test
    fun `sync switches from a disabled language to the device language`() = runTest {
        every { appLocaleManager.getSelectedLanguageCode(context) } returns "es"
        every { appLocaleManager.getDeviceLanguageTags() } returns listOf("en-GB")

        interactor.syncLanguages(context)

        verify { appLocaleManager.changeLanguage(context, "en") }
    }

    @Test
    fun `sync switches to the default language when no device language is enabled`() = runTest {
        every { appLocaleManager.getSelectedLanguageCode(context) } returns "es"

        interactor.syncLanguages(context)

        verify { appLocaleManager.changeLanguage(context, "fr") }
    }

    @Test
    fun `sync keeps an enabled language`() = runTest {
        every { appLocaleManager.getSelectedLanguageCode(context) } returns "hi"

        interactor.syncLanguages(context)

        verify(exactly = 0) { appLocaleManager.changeLanguage(any(), any()) }
    }

    @Test
    fun `sync keeps following an enabled device language`() = runTest {
        every { appLocaleManager.getSelectedLanguageCode(context) } returns null
        every { appLocaleManager.getDeviceLanguageTags() } returns listOf("fr-CM")

        interactor.syncLanguages(context)

        verify(exactly = 0) { appLocaleManager.changeLanguage(any(), any()) }
    }

    @Test
    fun `sync does nothing before the list was ever loaded`() = runTest {
        coEvery { repository.getLanguages() } returns BaseResult.Error(IOException())
        every { repository.getCachedLanguages() } returns null
        every { appLocaleManager.getSelectedLanguageCode(context) } returns "es"

        interactor.syncLanguages(context)

        verify(exactly = 0) { appLocaleManager.changeLanguage(any(), any()) }
        coVerify(exactly = 0) { repository.keepStrings(any()) }
    }

    @Test
    fun `sync saves the list and drops the strings of disabled languages`() = runTest {
        every { appLocaleManager.getSelectedLanguageCode(context) } returns "en"

        interactor.syncLanguages(context)

        coVerify { repository.saveLanguages(model) }
        coVerify { repository.keepStrings(listOf("en", "fr", "hi")) }
        verify { appLocaleManager.setSupportedLanguages(context, listOf("en", "fr", "hi")) }
    }

    @Test
    fun `updateStrings downloads a newer version`() = runTest {
        val strings = LanguageStringsModel(code = "hi", version = 2, strings = mapOf("email" to "ईमेल"))
        coEvery { repository.getStrings("hi") } returns BaseResult.Success(strings)

        interactor.updateStrings("hi")

        coVerify { repository.saveStrings(strings) }
    }

    @Test
    fun `updateStrings downloads strings never cached`() = runTest {
        coEvery { repository.getCachedStringsVersion("en") } returns null

        interactor.updateStrings("en")

        coVerify { repository.getStrings("en") }
    }

    @Test
    fun `updateStrings skips an up to date or unknown language`() = runTest {
        interactor.updateStrings("fr")
        interactor.updateStrings("de")

        coVerify(exactly = 0) { repository.getStrings(any()) }
    }

    @Test
    fun `changeLanguage downloads the strings before switching`() = runTest {
        interactor.changeLanguage(context, "hi")

        coVerify { repository.getStrings("hi") }
        verify { appLocaleManager.changeLanguage(context, "hi") }
    }
}
