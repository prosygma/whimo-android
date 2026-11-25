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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoFileParserTest {

    @Test
    fun `geoid lines skip header, blank and incomplete lines`() {
        val lines = sequenceOf(
            "latitude,longitude",
            "4.94,12.65",
            "",
            "   ",
            "4.95",
            " 4.96 , 12.66 ",
            "",
        )

        val points = parseGeoIdLines(lines)

        assertEquals(2, points.size)
        assertEquals(4.94, points[0].latitude(), 0.0)
        assertEquals(12.65, points[0].longitude(), 0.0)
        assertEquals(4.96, points[1].latitude(), 0.0)
        assertEquals(12.66, points[1].longitude(), 0.0)
    }

    @Test
    fun `geoid file without a valid line gives no point`() {
        assertTrue(parseGeoIdLines(sequenceOf("", "lat,lon", "x")).isEmpty())
    }
}
