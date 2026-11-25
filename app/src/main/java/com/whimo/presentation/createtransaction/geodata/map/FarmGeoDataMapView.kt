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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolygonAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.whimo.BuildConfig
import com.whimo.domain.createtransaction.models.GeoDataMapGeometry
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

private const val DEFAULT_ZOOM = 12.0
private const val FALLBACK_ZOOM = 10.0
private const val MAX_FIT_ZOOM = 18.0
private val FIT_PADDING = 40.dp

/**
 * Map preview of a plot (Mapbox version of EFI's Google Maps view).
 *
 * Interactive: the camera is free and its centre is reported through [onCameraTargetChanged]
 * (the screen draws a fixed pin over it). Otherwise the camera fits [mapGeometry] / [location]
 * and the plot is drawn as polygons, or as a marker when there is no polygon.
 */
@OptIn(FlowPreview::class)
@Composable
fun FarmGeoDataMapView(
    modifier: Modifier = Modifier,
    location: Point? = null,
    mapGeometry: GeoDataMapGeometry? = null,
    isInteractive: Boolean = false,
    gesturesEnabled: Boolean = isInteractive,
    isMyLocationEnabled: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(),
    onCameraTargetChanged: (Point) -> Unit = {},
) {
    val density = LocalDensity.current
    val defaultLocation = remember {
        Point.fromLngLat(BuildConfig.DEFAULT_LOCATION_LONGITUDE, BuildConfig.DEFAULT_LOCATION_LATITUDE)
    }
    val visiblePoints = remember(location, mapGeometry) {
        mapGeometry.visiblePoints(location).ifEmpty { listOf(defaultLocation) }
    }
    val bottomPadding = contentPadding.calculateBottomPadding()
    val cameraPadding = remember(bottomPadding, density) {
        EdgeInsets(0.0, 0.0, with(density) { bottomPadding.toPx().toDouble() }, 0.0)
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(visiblePoints.first())
            zoom(DEFAULT_ZOOM)
            padding(cameraPadding)
        }
    }

    LaunchedEffect(mapViewportState, isInteractive) {
        if (!isInteractive) return@LaunchedEffect

        snapshotFlow { mapViewportState.cameraState?.center }
            .debounce(200)
            .collect { center -> if (center != null) onCameraTargetChanged(center) }
    }

    MapboxMap(
        modifier = modifier,
        mapViewportState = mapViewportState,
        // Satellite imagery with labels: needed to identify actual plot boundaries.
        style = { MapStyle(style = Style.SATELLITE_STREETS) },
        scaleBar = {},
        compass = {},
    ) {
        MapEffect(gesturesEnabled) { mapView ->
            mapView.gestures.updateSettings {
                scrollEnabled = gesturesEnabled
                pinchToZoomEnabled = gesturesEnabled
                doubleTapToZoomInEnabled = gesturesEnabled
                doubleTouchToZoomOutEnabled = gesturesEnabled
                quickZoomEnabled = gesturesEnabled
                rotateEnabled = false
                pitchEnabled = false
            }
        }

        MapEffect(isInteractive, isMyLocationEnabled) { mapView ->
            mapView.location.updateSettings {
                enabled = isInteractive && isMyLocationEnabled
                locationPuck = createDefault2DPuck(withBearing = true)
                puckBearingEnabled = true
            }
        }

        MapEffect(visiblePoints, isInteractive) { mapView ->
            if (isInteractive) return@MapEffect
            val map = mapView.mapboxMap

            if (visiblePoints.size == 1) {
                map.setCamera(
                    CameraOptions.Builder()
                        .center(visiblePoints.first())
                        .zoom(DEFAULT_ZOOM)
                        .padding(cameraPadding)
                        .build()
                )
                return@MapEffect
            }

            val fit = with(density) { FIT_PADDING.toPx().toDouble() }
            val insets = EdgeInsets(fit, fit, fit + cameraPadding.bottom, fit)
            runCatching {
                // The async variant waits until the map has a size, like Google's onMapLoaded.
                map.cameraForCoordinates(
                    visiblePoints,
                    CameraOptions.Builder().build(),
                    insets,
                    MAX_FIT_ZOOM,
                    null,
                ) { camera -> map.setCamera(camera) }
            }.onFailure {
                map.setCamera(
                    CameraOptions.Builder().center(visiblePoints.first()).zoom(FALLBACK_ZOOM).build()
                )
            }
        }

        val polygonColor = MaterialTheme.colorScheme.primary
        mapGeometry?.polygons?.forEach { polygon ->
            if (polygon.points.size >= 3) {
                PolygonAnnotation(points = listOf(polygon.points)) {
                    fillColor = polygonColor
                    fillOpacity = 0.22
                }
                PolylineAnnotation(points = polygon.points.closedRing()) {
                    lineColor = polygonColor
                    lineWidth = 2.5
                }
            }
        }

        if (!isInteractive && mapGeometry?.polygons.isNullOrEmpty()) {
            (mapGeometry?.point ?: location)?.let { point ->
                CircleAnnotation(point = point) {
                    circleColor = polygonColor
                    circleRadius = 8.0
                    circleStrokeColor = Color.White
                    circleStrokeWidth = 2.0
                }
            }
        }
    }
}

private fun GeoDataMapGeometry?.visiblePoints(fallbackLocation: Point?): List<Point> {
    val geometry = this ?: return fallbackLocation?.let(::listOf) ?: emptyList()
    val polygonPoints = geometry.polygons.flatMap { it.points }

    return buildList {
        addAll(polygonPoints)
        geometry.point?.let(::add)
        if (geometry.isEmpty()) fallbackLocation?.let(::add)
    }
}

private fun List<Point>.closedRing(): List<Point> {
    if (isEmpty() || first() == last()) return this
    return this + first()
}

