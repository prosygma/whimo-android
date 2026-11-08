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
package com.whimo.data.appoptions

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppOptionsDescriptionsTest {

    @Test
    fun `backend text is used for its status`() {
        val descriptions = mapOf("full" to "Given by: scanning the CICC QR code.")

        assertEquals("Given by: scanning the CICC QR code.", descriptions.descriptionFor("Full"))
    }

    @Test
    fun `missing or blank text falls back to the app`() {
        assertNull(null.descriptionFor("full"))
        assertNull(mapOf("full" to " ").descriptionFor("full"))
        assertNull(mapOf("full" to "x").descriptionFor("partial"))
    }

    @Test
    fun `response with descriptions parses`() {
        val json = """{"success":true,"data":{"ask_on_plot":false,"allow_gps_point":true,"allow_cicc_qr":true,
            "traceability_descriptions":{"full":"A","conditional":"B","partial":"C","incomplete":"D"}}}"""

        val data = Gson().fromJson(json, AppOptionsResponse::class.java).data!!

        assertEquals(false, data.ask_on_plot)
        assertEquals("D", data.traceability_descriptions.descriptionFor("incomplete"))
    }
}
