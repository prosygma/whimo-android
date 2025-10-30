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
package com.whimo.presentation.createtransaction.geodata.qr

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.whimo.base.BaseViewModel
import com.whimo.base.CoreViewEvent
import com.whimo.data.base.common.onError
import com.whimo.data.base.common.onSuccess
import com.whimo.data.geodata.model.request.UpdateGeoDataRequest
import com.whimo.domain.createtransaction.models.LocationProvider
import com.whimo.domain.createtransaction.models.MFile
import com.whimo.domain.geodata.GeoDataInteractor
import com.whimo.domain.transactions.models.TransactionModel
import com.whimo.network.ErrorHandler
import com.whimo.providers.ResourceProvider
import com.whimo.utils.convertToGeoJson
import com.whimo.utils.parseCoordinates
import com.whimo.utils.parseGoogleLink
import com.whimo.utils.parseUTMCoordinates
import com.whimo.utils.writeQRDataToCacheFile
import java.io.File

class QrScanViewModel(
    private val resourceProvider: ResourceProvider,
    private val interactor: GeoDataInteractor,
    private val errorHandler: ErrorHandler,
) : BaseViewModel<QrScanContract.Binding>() {

    private var transactionId: String? = null

    override fun createBinding(): QrScanContract.Binding {
        return QrScanContract.Binding()
    }

    override fun handleEvents(event: CoreViewEvent) {
        super.handleEvents(event)
        when (event) {
            is QrScanContract.Event.OnCreate -> onCreate(event.transactionModel)
            is QrScanContract.Event.QrScanned -> qrScanned(event.context, event.qrData)
        }
    }

    override fun copyBinding(binding: QrScanContract.Binding): QrScanContract.Binding {
        return binding.copy()
    }

    private fun onCreate(transactionModel: TransactionModel) {
        this.transactionId = transactionModel.id
    }


    private fun qrScanned(context: Context, qrData: String) {
        val location = parseGoogleLink(qrData).firstOrNull()

        if (transactionId == null) {
            handleQr(qrData, location)
        } else {
            uploadQr(context, qrData, location)
        }
    }

    private fun handleQr(qrData: String, location: LatLng?) {
        if (location != null) {
            setEffect(QrScanContract.Effect.QrResult(qrData, location))

        } else {
            launch {
                setEffect(QrScanContract.Effect.ToggleLoader(true))

                val coordinates = parseCoordinates(qrData)

                if (coordinates.isNotEmpty()) {
                    setEffect(
                        QrScanContract.Effect.ToggleLoader(false),
                        QrScanContract.Effect.QrResult(qrData, coordinates.firstOrNull())
                    )

                } else {
                    val utmCoordinates = parseUTMCoordinates(qrData)

                    setEffect(
                        QrScanContract.Effect.ToggleLoader(false),
                        QrScanContract.Effect.QrResult(qrData, utmCoordinates.firstOrNull())
                    )
                }
            }
        }
    }

    private fun uploadQr(context: Context, qrData: String, location: LatLng?) {
        launch {
            setEffect(QrScanContract.Effect.ToggleLoader(true))

            var qrFile: Pair<File?, MFile?> = null to null

            val coordinates = parseCoordinates(qrData)

            if (coordinates.isNotEmpty()) {
                val geoJson = convertToGeoJson(qrData, location, coordinates)

                qrFile = writeQRDataToCacheFile(
                    context = context,
                    content = geoJson,
                )

            } else {
                val utmCoordinates = parseUTMCoordinates(qrData, location)
                val geoJson = convertToGeoJson(qrData, location, utmCoordinates)

                qrFile = writeQRDataToCacheFile(
                    context = context,
                    content = geoJson,
                )
            }

            interactor.updateTransactionGeoData(
                transactionId = transactionId!!,
                request = UpdateGeoDataRequest(
                    locationProvider = LocationProvider.QR,
                    locationFile = qrFile.second,
                )
            )
                .onSuccess {
                    qrFile.first?.delete()
                    setEffect(
                        QrScanContract.Effect.ToggleLoader(false),
                        QrScanContract.Effect.QrResult(qrData, location)
                    )
                }
                .onError {
                    qrFile.first?.delete()
                    it.printStackTrace()
                    val errorMessage = errorHandler.parseError(it)
                    setEffect(
                        QrScanContract.Effect.ToggleLoader(false),
                        QrScanContract.Effect.ShowMessage(errorMessage)
                    )
                }
        }
    }
}