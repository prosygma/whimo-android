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
import kotlin.math.*

object UtmConverter {

    private const val a = 6378137.0               // WGS84 semi-major axis
    private const val f = 1 / 298.257223563       // WGS84 flattening
    private const val k0 = 0.9996                 // scale factor

    private val e = sqrt(f * (2 - f))             // eccentricity

    fun utmToLatLng(
        easting: Double,
        northing: Double,
        zone: Int,
        isNorthernHemisphere: Boolean
    ): LatLng {
        val e1sq = e * e / (1 - e * e)
        val x = easting - 500000.0
        var y = northing
        if (!isNorthernHemisphere) {
            y -= 10000000.0
        }

        val m = y / k0
        val mu = m / (a * (1 - e * e / 4 - 3 * e.pow(4) / 64 - 5 * e.pow(6) / 256))

        val e1 = (1 - sqrt(1 - e * e)) / (1 + sqrt(1 - e * e))

        val j1 = 3 * e1 / 2 - 27 * e1.pow(3) / 32
        val j2 = 21 * e1.pow(2) / 16 - 55 * e1.pow(4) / 32
        val j3 = 151 * e1.pow(3) / 96
        val j4 = 1097 * e1.pow(4) / 512

        val fp = mu + j1 * sin(2 * mu) + j2 * sin(4 * mu) + j3 * sin(6 * mu) + j4 * sin(8 * mu)

        val sinfp = sin(fp)
        val cosfp = cos(fp)
        val tanfp = tan(fp)

        val c1 = e1sq * cosfp * cosfp
        val t1 = tanfp * tanfp
        val n1 = a / sqrt(1 - e * e * sinfp * sinfp)
        val r1 = a * (1 - e * e) / (1 - e * e * sinfp * sinfp).pow(1.5)
        val d = x / (n1 * k0)

        val q1 = n1 * tanfp / r1
        val q2 = d * d / 2
        val q3 = (5 + 3 * t1 + 10 * c1 - 4 * c1 * c1 - 9 * e1sq) * d.pow(4) / 24
        val q4 = (61 + 90 * t1 + 298 * c1 + 45 * t1 * t1 - 252 * e1sq - 3 * c1 * c1) * d.pow(6) / 720
        val lat = fp - q1 * (q2 - q3 + q4)

        val q5 = d
        val q6 = (1 + 2 * t1 + c1) * d.pow(3) / 6
        val q7 = (5 - 2 * c1 + 28 * t1 - 3 * c1 * c1 + 8 * e1sq + 24 * t1 * t1) * d.pow(5) / 120
        val lon = (dToR(((zone - 1) * 6 - 180 + 3).toDouble())) + (q5 - q6 + q7) / cosfp

        return LatLng(Math.toDegrees(lat), Math.toDegrees(lon))
    }

    private fun dToR(deg: Double) = deg * Math.PI / 180.0
}