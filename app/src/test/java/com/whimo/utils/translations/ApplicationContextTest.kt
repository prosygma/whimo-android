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
import com.whimo.WhimoApp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
// Later SDKs are skipped: this project runs Robolectric in legacy resources mode.
@Config(sdk = [28])
class ApplicationContextTest {

    // The framework's ContextImpl. WhimoApp.onCreate is not run: it needs Mapbox's native code.
    private val systemContext: Context = RuntimeEnvironment.getApplication().baseContext

    private val application = WhimoApp().also {
        ReflectionHelpers.callInstanceMethod<Unit>(
            it,
            "attachBaseContext",
            ClassParameter.from(Context::class.java, systemContext),
        )
    }

    @Test
    fun `application base context is not wrapped`() {
        // ActivityThread.handleReceiver casts it to ContextImpl.
        assertSame(systemContext, application.baseContext)
        assertFalse(application.baseContext is TranslatedContextWrapper)
    }

    @Test
    fun `application resources still return downloaded strings`() {
        assertTrue(application.resources is TranslatedResources)
    }
}
