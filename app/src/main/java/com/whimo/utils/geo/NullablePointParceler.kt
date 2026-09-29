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
package com.whimo.utils.geo

import android.os.Parcel
import com.mapbox.geojson.Point
import kotlinx.parcelize.Parceler

/**
 * Mapbox's [Point] is Serializable but not Parcelable, so @Parcelize models that
 * carry a location need an explicit parceler.
 *
 * Written as longitude-then-latitude to match [Point.fromLngLat] and avoid any
 * chance of the pair being reassembled in the wrong order.
 */
object NullablePointParceler : Parceler<Point?> {

    override fun create(parcel: Parcel): Point? {
        val hasValue = parcel.readInt() == 1
        if (!hasValue) return null
        val longitude = parcel.readDouble()
        val latitude = parcel.readDouble()
        return Point.fromLngLat(longitude, latitude)
    }

    override fun Point?.write(parcel: Parcel, flags: Int) {
        if (this == null) {
            parcel.writeInt(0)
            return
        }
        parcel.writeInt(1)
        parcel.writeDouble(longitude())
        parcel.writeDouble(latitude())
    }
}
