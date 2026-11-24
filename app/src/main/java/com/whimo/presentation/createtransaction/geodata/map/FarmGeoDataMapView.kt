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
package com.whimo.presentation.createtransaction.geodata.map

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.whimo.BuildConfig
import com.whimo.domain.createtransaction.models.GeoDataMapGeometry
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(FlowPreview::class)
@Composable
fun FarmGeoDataMapView(
    modifier: Modifier = Modifier,
    location: LatLng? = null,
    mapGeometry: GeoDataMapGeometry? = null,
    isInteractive: Boolean = false,
    gesturesEnabled: Boolean = isInteractive,
    isMyLocationEnabled: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(),
    onCameraTargetChanged: (LatLng) -> Unit = {},
) {
    val defaultLocation = remember {
        LatLng(BuildConfig.DEFAULT_LOCATION_LATITUDE, BuildConfig.DEFAULT_LOCATION_LONGITUDE)
    }
    val visiblePoints = remember(location, mapGeometry) {
        mapGeometry.visiblePoints(location).ifEmpty { listOf(defaultLocation) }
    }
    val initialLocation = visiblePoints.first()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 12f)
    }
    var mapLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(cameraPositionState, isInteractive) {
        if (!isInteractive) return@LaunchedEffect

        snapshotFlow { cameraPositionState.position.target }
            .debounce(200)
            .collect { onCameraTargetChanged(it) }
    }

    LaunchedEffect(mapLoaded, visiblePoints, isInteractive) {
        if (!mapLoaded || isInteractive) return@LaunchedEffect

        if (visiblePoints.size == 1) {
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(visiblePoints.first(), 12f))
            return@LaunchedEffect
        }

        val bounds = visiblePoints.toBounds()
        runCatching {
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 80))
        }.onFailure {
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(visiblePoints.first(), 10f))
        }
    }

    val uiSettings = MapUiSettings(
        mapToolbarEnabled = false,
        myLocationButtonEnabled = isInteractive && isMyLocationEnabled,
        scrollGesturesEnabled = gesturesEnabled,
        zoomGesturesEnabled = gesturesEnabled,
        tiltGesturesEnabled = false,
        rotationGesturesEnabled = false,
        zoomControlsEnabled = false,
    )

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = isInteractive && isMyLocationEnabled),
        uiSettings = uiSettings,
        contentPadding = contentPadding,
        onMapLoaded = {
            mapLoaded = true
        },
    ) {
        val polygonColor = MaterialTheme.colorScheme.primary
        mapGeometry?.polygons?.forEach { polygon ->
            if (polygon.points.size >= 3) {
                Polygon(
                    points = polygon.points,
                    fillColor = polygonColor.copy(alpha = 0.22f),
                    strokeColor = polygonColor,
                    strokeWidth = 5f,
                )
            }
        }

        if (!isInteractive && mapGeometry?.polygons.isNullOrEmpty()) {
            (mapGeometry?.point ?: location)?.let { point ->
                Marker(state = MarkerState(position = point))
            }
        }
    }
}

private fun GeoDataMapGeometry?.visiblePoints(fallbackLocation: LatLng?): List<LatLng> {
    val geometry = this ?: return fallbackLocation?.let(::listOf) ?: emptyList()
    val polygonPoints = geometry.polygons.flatMap { it.points }

    return buildList {
        addAll(polygonPoints)
        geometry.point?.let(::add)
        if (isEmpty()) fallbackLocation?.let(::add)
    }
}

private fun List<LatLng>.toBounds(): LatLngBounds {
    val builder = LatLngBounds.builder()
    forEach(builder::include)
    return builder.build()
}
