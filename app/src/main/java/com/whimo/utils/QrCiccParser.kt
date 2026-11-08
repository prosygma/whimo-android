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

import com.google.gson.Gson
import java.text.Normalizer

/**
 * Reads the QR code printed on a CICC plot identification card ("Fiche d'identification et de
 * géolocalisation des parcelles cacaoyères"). The code is plain text, not GeoJSON:
 *
 * ```
 * NIPr_CICC :XX.AAA.BBB0001
 * GLN_Parcelle :0000000000001
 * Lien_GS1_Parcelle :https://example.org/414/0000000000001
 * Coordonnées :
 * [4.1;12.1],[4.1005;12.1004],...
 * ```
 *
 * Each point is `[latitude;longitude]`. The plot becomes a GeoJSON polygon whose feature
 * properties carry the CICC keys, which the backend checks.
 */
object QrCiccParser {

    const val KEY_NIPR = "NIPr_CICC"
    const val KEY_GLN = "GLN_Parcelle"
    const val KEY_GS1_LINK = "Lien_GS1_Parcelle"
    const val KEY_COORDINATES = "Coordonnees"

    val REQUIRED_KEYS = listOf(KEY_NIPR, KEY_GLN, KEY_GS1_LINK)

    private const val MIN_CORNERS = 3

    private val keyLine = Regex("""^\s*([A-Za-z][A-Za-z0-9_À-ÿ]*)\s*:(.*)$""")
    private val point = Regex("""\[\s*(-?\d+(?:[.,]\d+)?)\s*;\s*(-?\d+(?:[.,]\d+)?)\s*]""")

    sealed class Result {
        data class Success(
            val geoJson: String,
            /** First corner, as (latitude, longitude). */
            val firstCorner: Pair<Double, Double>,
            val properties: Map<String, String>,
        ) : Result()

        /** The listed keys are missing; [KEY_COORDINATES] also means "fewer than 3 valid points". */
        data class Invalid(val missingKeys: List<String>) : Result()
    }

    fun parse(text: String): Result {
        val fields = readFields(text)
        val missing = REQUIRED_KEYS.filter { normalise(it) !in fields }.toMutableList()

        val corners = fields[normalise(KEY_COORDINATES)]
            ?.let { value -> point.findAll(value).mapNotNull { it.toCorner() }.toList() }
            .orEmpty()
        if (corners.size < MIN_CORNERS) missing += KEY_COORDINATES

        if (missing.isNotEmpty()) return Result.Invalid(missing)

        val properties = REQUIRED_KEYS.associateWith { fields.getValue(normalise(it)) }
        return Result.Success(
            geoJson = toGeoJson(corners, properties),
            firstCorner = corners.first(),
            properties = properties,
        )
    }

    /** Values by normalised key. A value runs until the next "Key :" line. */
    private fun readFields(text: String): Map<String, String> {
        val fields = linkedMapOf<String, StringBuilder>()
        var current: StringBuilder? = null
        text.lines().forEach { line ->
            val match = keyLine.find(line)
            if (match != null) {
                current = StringBuilder(match.groupValues[2].trim())
                fields[normalise(match.groupValues[1])] = current!!
            } else {
                current?.append('\n')?.append(line.trim())
            }
        }
        return fields.mapValues { it.value.toString().trim() }
    }

    private fun MatchResult.toCorner(): Pair<Double, Double>? {
        val latitude = groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val longitude = groupValues[2].replace(',', '.').toDoubleOrNull() ?: return null
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        return latitude to longitude
    }

    private fun toGeoJson(corners: List<Pair<Double, Double>>, properties: Map<String, String>): String {
        val ring = corners.map { (lat, lng) -> listOf(lng, lat) }.toMutableList()
        if (ring.first() != ring.last()) ring += ring.first()

        val feature = mapOf(
            "type" to "Feature",
            "properties" to properties + ("Source" to "CICC"),
            "geometry" to mapOf("type" to "Polygon", "coordinates" to listOf(ring)),
        )
        return Gson().toJson(mapOf("type" to "FeatureCollection", "features" to listOf(feature)))
    }

    /** "Coordonnées " -> "coordonnees": keys are matched without accents, case or spaces. */
    private fun normalise(key: String): String {
        return Normalizer.normalize(key.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
    }
}
