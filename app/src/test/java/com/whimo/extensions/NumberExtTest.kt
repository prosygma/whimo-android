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
package com.whimo.extensions

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberExtTest {

    @Test
    fun `toQuantityTextWithSign formats negative value without extra sign`() {
        assertEquals("-50", (-50f).toQuantityTextWithSign())
    }

    @Test
    fun `toQuantityTextWithSign formats positive value with plus and no space`() {
        assertEquals("+20", 20f.toQuantityTextWithSign())
    }

    @Test
    fun `toQuantityTextWithSign formats zero without sign`() {
        assertEquals("0", 0f.toQuantityTextWithSign())
    }

    @Test
    fun `toQuantityTextWithUnit formats quantity and unit without space`() {
        assertEquals("50kg", 50f.toQuantityTextWithUnit("kg"))
    }

    @Test
    fun `toQuantityTextWithSignAndUnit formats signed quantity and unit without space`() {
        assertEquals("+20kgs", 20f.toQuantityTextWithSignAndUnit("kgs"))
        assertEquals("-50kgs", (-50f).toQuantityTextWithSignAndUnit("kgs"))
    }
}
