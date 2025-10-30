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

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Test

class UtmConverterTest {

    @Test
    fun `utmToLatLng converts UTM coordinates to LatLng correctly`() {
        // Given
        val easting = 500000.0
        val northing = 0.0
        val zone = 1
        val isNorthernHemisphere = true

        // When
        val result = UtmConverter.utmToLatLng(easting, northing, zone, isNorthernHemisphere)

        // Then
        assertNotNull(result)
        assertTrue(result.latitude >= -90.0 && result.latitude <= 90.0)
        assertTrue(result.longitude >= -180.0 && result.longitude <= 180.0)
    }

    @Test
    fun `utmToLatLng with northern hemisphere returns correct latitude`() {
        // Given
        val easting = 500000.0
        val northing = 1000000.0
        val zone = 32
        val isNorthernHemisphere = true

        // When
        val result = UtmConverter.utmToLatLng(easting, northing, zone, isNorthernHemisphere)

        // Then
        assertTrue(result.latitude > 0) // Should be in northern hemisphere
    }

    @Test
    fun `utmToLatLng with southern hemisphere returns correct latitude`() {
        // Given
        val easting = 500000.0
        val northing = 1000000.0
        val zone = 32
        val isNorthernHemisphere = false

        // When
        val result = UtmConverter.utmToLatLng(easting, northing, zone, isNorthernHemisphere)

        // Then
        assertTrue(result.latitude < 0) // Should be in southern hemisphere
    }

    @Test
    fun `utmToLatLng with different zones returns different longitudes`() {
        // Given
        val easting = 500000.0
        val northing = 1000000.0
        val isNorthernHemisphere = true

        // When
        val resultZone1 = UtmConverter.utmToLatLng(easting, northing, 1, isNorthernHemisphere)
        val resultZone32 = UtmConverter.utmToLatLng(easting, northing, 32, isNorthernHemisphere)

        // Then
        assertNotEquals(resultZone1.longitude, resultZone32.longitude, 0.001)
    }

    @Test
    fun `utmToLatLng with zero coordinates returns valid result`() {
        // Given
        val easting = 0.0
        val northing = 0.0
        val zone = 1
        val isNorthernHemisphere = true

        // When
        val result = UtmConverter.utmToLatLng(easting, northing, zone, isNorthernHemisphere)

        // Then
        assertNotNull(result)
        assertTrue(result.latitude >= -90.0 && result.latitude <= 90.0)
        assertTrue(result.longitude >= -180.0 && result.longitude <= 180.0)
    }

    @Test
    fun `utmToLatLng with large coordinates returns valid result`() {
        // Given
        val easting = 1000000.0
        val northing = 10000000.0
        val zone = 60
        val isNorthernHemisphere = true

        // When
        val result = UtmConverter.utmToLatLng(easting, northing, zone, isNorthernHemisphere)

        // Then
        assertNotNull(result)
        assertTrue(result.latitude >= -90.0 && result.latitude <= 90.0)
        assertTrue(result.longitude >= -180.0 && result.longitude <= 180.0)
    }

    @Test
    fun `utmToLatLng with negative easting returns valid result`() {
        // Given
        val easting = -100000.0
        val northing = 1000000.0
        val zone = 1
        val isNorthernHemisphere = true

        // When
        val result = UtmConverter.utmToLatLng(easting, northing, zone, isNorthernHemisphere)

        // Then
        assertNotNull(result)
        assertTrue(result.latitude >= -90.0 && result.latitude <= 90.0)
        assertTrue(result.longitude >= -180.0 && result.longitude <= 180.0)
    }

    @Test
    fun `utmToLatLng with edge zone values returns valid result`() {
        // Given
        val easting = 500000.0
        val northing = 1000000.0
        val isNorthernHemisphere = true

        // When
        val resultZone1 = UtmConverter.utmToLatLng(easting, northing, 1, isNorthernHemisphere)
        val resultZone60 = UtmConverter.utmToLatLng(easting, northing, 60, isNorthernHemisphere)

        // Then
        assertNotNull(resultZone1)
        assertNotNull(resultZone60)
        assertTrue(resultZone1.latitude >= -90.0 && resultZone1.latitude <= 90.0)
        assertTrue(resultZone60.latitude >= -90.0 && resultZone60.latitude <= 90.0)
    }

    @Test
    fun `utmToLatLng returns consistent results for same input`() {
        // Given
        val easting = 500000.0
        val northing = 1000000.0
        val zone = 32
        val isNorthernHemisphere = true

        // When
        val result1 = UtmConverter.utmToLatLng(easting, northing, zone, isNorthernHemisphere)
        val result2 = UtmConverter.utmToLatLng(easting, northing, zone, isNorthernHemisphere)

        // Then
        assertEquals(result1.latitude, result2.latitude, 0.000001)
        assertEquals(result1.longitude, result2.longitude, 0.000001)
    }
}
