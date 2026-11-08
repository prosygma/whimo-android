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

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCiccParserTest {

    // Made-up values, laid out like a real CICC card.
    private val sample = """
        PROGRAMME D'IDENTIFICATION ET DE GEOLOCALISATION DES PARCELLES CACAOYERES

        NIPr_CICC :XX.AAA.BBB0001
        GLN_Parcelle :0000000000001
        Lien_GS1_Parcelle :https://example.org/414/0000000000001
        Coordonnées : 
        [4.1;12.1],[4.1005;12.1004],[4.101;12.1],[4.1012;12.1006],[4.1008;12.101],[4.1004;12.1012],[4.1;12.1008],[4.0998;12.1004],[4.0996;12.1002],[4.0997;12.1],[4.0998;12.0998],[4.0999;12.0999]
    """.trimIndent()

    @Test
    fun `sample card becomes a closed polygon in longitude latitude order`() {
        val result = QrCiccParser.parse(sample) as QrCiccParser.Result.Success

        val feature = JsonParser.parseString(result.geoJson).asJsonObject
            .getAsJsonArray("features")[0].asJsonObject
        val ring = feature.getAsJsonObject("geometry").getAsJsonArray("coordinates")[0].asJsonArray

        assertEquals("Polygon", feature.getAsJsonObject("geometry")["type"].asString)
        assertEquals(13, ring.size()) // 12 corners + closing point
        assertEquals(12.1, ring[0].asJsonArray[0].asDouble, 0.0)
        assertEquals(4.1, ring[0].asJsonArray[1].asDouble, 0.0)
        assertEquals(ring[0], ring[12])
        assertEquals(4.1 to 12.1, result.firstCorner)

        val properties = feature.getAsJsonObject("properties")
        assertEquals("XX.AAA.BBB0001", properties["NIPr_CICC"].asString)
        assertEquals("0000000000001", properties["GLN_Parcelle"].asString)
        assertEquals("https://example.org/414/0000000000001", properties["Lien_GS1_Parcelle"].asString)
        assertEquals("CICC", properties["Source"].asString)
    }

    @Test
    fun `generated GeoJSON is accepted by the generic QR parser`() {
        val result = QrCiccParser.parse(sample) as QrCiccParser.Result.Success

        val qrGeoData = parseQrGeoData(result.geoJson)

        assertEquals(result.geoJson, qrGeoData.geoJson)
        assertEquals(12.1, qrGeoData.location!!.longitude(), 0.0)
        assertEquals(4.1, qrGeoData.location!!.latitude(), 0.0)
    }

    @Test
    fun `keys match without accents or case and points may span lines`() {
        val text = """
            nipr_cicc : X
            GLN_PARCELLE: 1
            lien_gs1_parcelle :https://example.org/a:b
            COORDONNEES :
            [3.1;11.1],
            [3.2;11.2]
            [3.3;11.1]
        """.trimIndent()

        val result = QrCiccParser.parse(text) as QrCiccParser.Result.Success

        assertEquals("https://example.org/a:b", result.properties[QrCiccParser.KEY_GS1_LINK])
        assertEquals("X", result.properties[QrCiccParser.KEY_NIPR])
    }

    @Test
    fun `missing coordinates are rejected`() {
        val text = sample.lines().takeWhile { !it.startsWith("Coordonn") }.joinToString("\n")

        val result = QrCiccParser.parse(text) as QrCiccParser.Result.Invalid

        assertEquals(listOf(QrCiccParser.KEY_COORDINATES), result.missingKeys)
    }

    @Test
    fun `fewer than three valid points are rejected`() {
        val text = sample.replace(Regex("""Coordonnées :[\s\S]*"""), "Coordonnées : [3.1;11.1],[95.0;11.2],[3.3;11.3]")

        val result = QrCiccParser.parse(text) as QrCiccParser.Result.Invalid

        assertEquals(listOf(QrCiccParser.KEY_COORDINATES), result.missingKeys)
    }

    @Test
    fun `each missing CICC key is reported`() {
        val text = sample.lines().filterNot { it.startsWith("GLN_") || it.startsWith("Lien_") }.joinToString("\n")

        val result = QrCiccParser.parse(text) as QrCiccParser.Result.Invalid

        assertEquals(listOf(QrCiccParser.KEY_GLN, QrCiccParser.KEY_GS1_LINK), result.missingKeys)
    }

    @Test
    fun `a GeoJSON QR code is not a CICC code`() {
        val result = QrCiccParser.parse("""{"type":"Feature","geometry":{"type":"Point","coordinates":[1,2]}}""")

        assertTrue(result is QrCiccParser.Result.Invalid)
    }
}
