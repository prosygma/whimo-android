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

import android.annotation.SuppressLint
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.whimo.BuildConfig
import com.whimo.R
import com.whimo.domain.createtransaction.models.LocationProvider
import com.whimo.extensions.findActivity
import com.whimo.extensions.toText
import com.whimo.presentation.createtransaction.geodata.FarmGeoDataActivity
import com.whimo.presentation.main.components.Toolbar2
import com.whimo.presentation.ui.baseScreen.MainButton
import com.whimo.presentation.ui.components.BaseTextField
import com.whimo.utils.LocationPermissionRequester
import kotlinx.coroutines.flow.debounce

private const val DEFAULT_ZOOM = 12.0

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    modifier: Modifier,
    navController: NavHostController,
) {
    val context = LocalContext.current

    var isMyLocationEnabled by remember { mutableStateOf(false) }

    LocationPermissionRequester { result ->
        isMyLocationEnabled = result
    }

    Column(modifier = modifier) {
        Toolbar2(
            navController = navController,
            title = stringResource(R.string.farm_geodata),
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val iconSize = 46.dp
            val contentPadding = 148.dp

            val defaultPoint = remember {
                Point.fromLngLat(
                    BuildConfig.DEFAULT_LOCATION_LONGITUDE,
                    BuildConfig.DEFAULT_LOCATION_LATITUDE,
                )
            }

            var currentPoint by remember { mutableStateOf(defaultPoint) }

            val mapViewportState = rememberMapViewportState {
                setCameraOptions {
                    center(defaultPoint)
                    zoom(DEFAULT_ZOOM)
                }
            }

            // The pin is fixed at the centre of the viewport, so the selected
            // coordinate is whatever the camera is currently centred on.
            LaunchedEffect(mapViewportState) {
                snapshotFlow { mapViewportState.cameraState?.center }
                    .debounce(200)
                    .collect { center ->
                        if (center != null) currentPoint = center
                    }
            }

            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
                // Satellite imagery with labels: needed to identify actual plot boundaries.
                style = { MapStyle(style = Style.SATELLITE_STREETS) },
            ) {
                MapEffect(isMyLocationEnabled) { mapView ->
                    mapView.location.updateSettings {
                        enabled = isMyLocationEnabled
                        locationPuck = createDefault2DPuck(withBearing = true)
                        puckBearingEnabled = true
                    }
                }
            }

            Icon(
                modifier = Modifier
                    .size(iconSize)
                    .align(Alignment.Center)
                    .offset(y = -contentPadding / 2 - iconSize / 2),
                painter = painterResource(id = R.drawable.ic_map_pin),
                contentDescription = stringResource(R.string.marker_icon),
                tint = Color.Unspecified,
            )

            BaseTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 80.dp)
                    .height(48.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),

                isReadOnly = true,

                text = currentPoint.toText(),

                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(id = R.drawable.ic_map_marker),
                        contentDescription = stringResource(R.string.marker_icon),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },

                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.outline,

                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                errorContainerColor = MaterialTheme.colorScheme.surface,
            )

            MainButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                title = stringResource(R.string.confirm),
                onClick = {
                    val activity = context.findActivity() as FarmGeoDataActivity
                    activity.setResult(
                        locationProvider = LocationProvider.Manual,
                        location = currentPoint,
                    )
                    activity.finish()
                }
            )
        }
    }
}
