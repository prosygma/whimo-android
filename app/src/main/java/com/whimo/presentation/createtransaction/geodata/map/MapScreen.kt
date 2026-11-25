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
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mapbox.geojson.Point
import com.whimo.BuildConfig
import com.whimo.R
import com.whimo.base.ObserveEffects
import com.whimo.domain.createtransaction.models.GeoDataMapGeometry
import com.whimo.domain.createtransaction.models.LocationProvider
import com.whimo.domain.transactions.models.TransactionModel
import com.whimo.extensions.findActivity
import com.whimo.extensions.toText
import com.whimo.presentation.createtransaction.geodata.FarmGeoDataActivity
import com.whimo.presentation.createtransaction.geodata.qr.QrScanContract
import com.whimo.presentation.createtransaction.geodata.qr.QrScanViewModel
import com.whimo.presentation.main.components.LoadingState
import com.whimo.presentation.main.components.Toolbar2
import com.whimo.presentation.ui.baseScreen.MainButton
import com.whimo.presentation.ui.components.BaseTextField
import com.whimo.presentation.ui.theme.WhimoTheme
import com.whimo.utils.LocationPermissionRequester
import org.koin.androidx.compose.koinViewModel

@Preview
@Composable
private fun Preview() {
    WhimoTheme {
        MapScreen(
            modifier = Modifier.fillMaxSize(),
            navController = rememberNavController(),
            viewModel = null,
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    modifier: Modifier,
    navController: NavHostController,
    transactionModel: TransactionModel? = null,
    qrData: String? = null,
    qrLocation: Point? = null,
    qrMapGeometry: GeoDataMapGeometry? = null,
    viewModel: QrScanViewModel? = koinViewModel(),
) {
    val context = LocalContext.current
    val isQrConfirmation = qrData != null

    var isMyLocationEnabled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var currentLatLng by remember {
        mutableStateOf(
            Point.fromLngLat(
                BuildConfig.DEFAULT_LOCATION_LONGITUDE,
                BuildConfig.DEFAULT_LOCATION_LATITUDE,
            )
        )
    }

    if (!isQrConfirmation) {
        LocationPermissionRequester { result ->
            isMyLocationEnabled = result
        }
    }

    if (isQrConfirmation && viewModel != null) {
        ObserveEffects(viewModel) { effect ->
            when (effect) {
                is QrScanContract.Effect.QrResult -> {
                    val activity = context.findActivity() as FarmGeoDataActivity
                    activity.setResult(
                        locationProvider = effect.provider,
                        location = effect.location,
                        mapGeometry = effect.mapGeometry,
                        qrData = effect.qrData,
                    )
                    activity.finish()
                }
                is QrScanContract.Effect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }
                is QrScanContract.Effect.ToggleLoader -> {
                    isLoading = effect.isLoading
                }
                is QrScanContract.Effect.ShowQrConfirmation -> Unit
            }
        }
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_CREATE) {
        if (isQrConfirmation && transactionModel != null) {
            viewModel?.setEvent(QrScanContract.Event.OnCreate(transactionModel))
        }
    }

    val contentPadding = 148.dp
    val confirmedLocation = if (isQrConfirmation) {
        qrLocation ?: qrMapGeometry?.point
    } else {
        currentLatLng
    }
    val confirmedGeometry = if (isQrConfirmation) {
        qrMapGeometry
    } else {
        GeoDataMapGeometry(point = currentLatLng)
    }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Toolbar2(
                navController = navController,
                title = stringResource(if (isQrConfirmation) R.string.farm_geodata else R.string.add_gps_point),
                showOfflineBanner = false,
            )

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val iconSize = 46.dp

                FarmGeoDataMapView(
                    modifier = Modifier.fillMaxSize(),
                    location = confirmedLocation,
                    mapGeometry = qrMapGeometry,
                    isInteractive = !isQrConfirmation,
                    gesturesEnabled = true,
                    isMyLocationEnabled = isMyLocationEnabled,
                    contentPadding = PaddingValues(bottom = contentPadding),
                    onCameraTargetChanged = {
                        currentLatLng = it
                    },
                )

                if (!isQrConfirmation) {
                    Icon(
                        modifier = Modifier
                            .size(iconSize)
                            .align(Alignment.Center)
                            .offset(y = -contentPadding / 2 - iconSize / 2),
                        painter = painterResource(id = R.drawable.ic_map_pin),
                        contentDescription = stringResource(R.string.marker_icon),
                        tint = Color.Unspecified,
                    )
                }

                BaseTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = 80.dp)
                        .height(48.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),

                    isReadOnly = true,

                    text = confirmedLocation?.toText().orEmpty(),

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
                        if (isQrConfirmation) {
                            viewModel?.setEvent(
                                QrScanContract.Event.ConfirmQr(
                                    context = context,
                                    qrData = qrData.orEmpty(),
                                    location = confirmedLocation,
                                    mapGeometry = confirmedGeometry,
                                )
                            )
                        } else {
                            val activity = context.findActivity() as FarmGeoDataActivity
                            activity.setResult(
                                locationProvider = LocationProvider.Manual,
                                location = currentLatLng,
                                mapGeometry = confirmedGeometry,
                            )
                            activity.finish()
                        }
                    }
                )
            }
        }

        if (isLoading) {
            LoadingState()
        }
    }
}
