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

import android.content.Context
import androidx.core.net.toUri
import com.google.android.gms.maps.model.LatLng
import com.whimo.domain.createtransaction.models.MFile
import java.io.File

data class GeoJsonFeatureCollection(
    val type: String = "FeatureCollection",
    val features: List<GeoJsonFeature>
)

data class GeoJsonFeature(
    val type: String = "Feature",
    val geometry: GeoJsonGeometry,
    val properties: Map<String, Any?> = emptyMap()
)

sealed class GeoJsonGeometry(val type: String) {
    data class GeoJsonPointGeometry(
        val coordinates: List<Double>
    ) : GeoJsonGeometry("Point")

    data class GeoJsonPolygonGeometry(
        val coordinates: List<List<List<Double>>>
    ) : GeoJsonGeometry("Polygon")
}


fun convertToGeoJson(qr: String, referenceCoordinate: LatLng?, utmCoordinates: List<LatLng>?): String {
    val properties = extractProperties(qr)

    val features = mutableListOf<GeoJsonFeature>()

//    if (referenceCoordinate != null) {
//        features.add(
//            GeoJsonFeature(
//                geometry = GeoJsonGeometry.GeoJsonPointGeometry(
//                    coordinates = listOf(
//                        referenceCoordinate.longitude,
//                        referenceCoordinate.latitude
//                    )
//                ),
//                properties = properties
//            )
//        )
//    }

    if (!utmCoordinates.isNullOrEmpty()) {
        var closedUTMCoordinates = utmCoordinates

        if (utmCoordinates.first() != utmCoordinates.last()) {
            closedUTMCoordinates = utmCoordinates + utmCoordinates.first()
        }

        val polygonCoordinates = closedUTMCoordinates.map { listOf(it.longitude, it.latitude) }

        features.add(
            GeoJsonFeature(
                geometry = GeoJsonGeometry.GeoJsonPolygonGeometry(
                    coordinates = listOf(polygonCoordinates)
                ),
                properties = properties
            )
        )
    }

    return GeoJsonFeatureCollection(features = features).toJsonArgs()
}

private fun extractProperties(text: String): Map<String, Any?> {
    return text
        .lines()
        .mapNotNull { line ->
            if (line.startsWith("https://")) return@mapNotNull null

            val parts = line.split(":", limit = 2)

            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()

                return@mapNotNull key to value
            } else {
                return@mapNotNull null
            }
        }
        .toMap()
}

fun parseGoogleLink(qr: String): List<LatLng> {
    val googleMapsRegex = Regex("""query=(-?\d+\.\d+),(-?\d+\.\d+)""")

    val results = mutableListOf<LatLng>()

    googleMapsRegex.findAll(qr).forEach {
        val lat = it.groupValues[1].toDouble()
        val lon = it.groupValues[2].toDouble()
        results.add(LatLng(lat, lon))
    }

    return results
}

fun parseUTMCoordinates(qr: String, referenceCoordinate: LatLng? = null): List<LatLng> {
    val utmRegex = Regex("""\[\s*(-?\d+(?:\.\d+)?)\s*;\s*(-?\d+(?:\.\d+)?)\s*]""")

    val results = mutableListOf<LatLng>()

    val isNorthern = referenceCoordinate == null || referenceCoordinate.latitude >= 0
    val utmZone = referenceCoordinate?.longitude?.let { getUTMZone(it) } ?: 32 // 32 -> Cameroon

    utmRegex.findAll(qr).forEach {
        val x = it.groupValues[1].toDouble()
        val y = it.groupValues[2].toDouble()

        try {
            val latLng = UtmConverter.utmToLatLng(x, y, utmZone, isNorthern)
            results.add(latLng)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    return results
}

fun parseCoordinates(qr: String): List<LatLng> {
    val utmRegex = Regex("""\[\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*]""")

    val results = mutableListOf<LatLng>()

    utmRegex.findAll(qr).forEach {
        val lat = it.groupValues[1].toDouble()
        val lon = it.groupValues[2].toDouble()

        try {
            results.add(LatLng(lat, lon))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    return results
}

private fun getUTMZone(longitude: Double): Int {
    return ((longitude + 180) / 6).toInt() + 1
}

fun writeQRDataToCacheFile(
    context: Context,
    content: String
): Pair<File?, MFile?> {
    try {
        val fileName = "qr.geojson"
        val file = File(context.cacheDir, fileName)
        file.writeText(content)

        return file to MFile(file.toUri(), fileName, file.length())
    } catch (e: Exception) {
        e.printStackTrace()
        return null to null
    }
}