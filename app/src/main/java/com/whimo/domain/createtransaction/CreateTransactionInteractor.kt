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
package com.whimo.domain.createtransaction

import android.content.Context
import com.whimo.data.base.common.BaseResult
import com.whimo.data.createtransaction.model.entity.PendingTransactionEntity
import com.whimo.data.createtransaction.model.mappers.toEntity
import com.whimo.data.createtransaction.model.mappers.toModel
import com.whimo.data.createtransaction.model.request.CreateDownstreamTransactionRequest
import com.whimo.data.createtransaction.model.request.CreateProducerTransactionRequest
import com.whimo.data.createtransaction.repository.CreateTransactionRepository
import com.whimo.domain.createtransaction.models.CreateTransactionModel
import com.whimo.domain.createtransaction.models.LocationProvider
import com.whimo.domain.createtransaction.models.MFile
import com.whimo.domain.createtransaction.models.PendingTransactionModel
import com.whimo.domain.createtransaction.models.UserInfoModel
import com.whimo.domain.transactions.models.TransactionModel
import com.whimo.extensions.isNetworkAvailable
import com.whimo.utils.parseQrGeoData
import com.whimo.utils.toJsonArgs
import com.whimo.utils.writeQRDataToCacheFile
import java.io.File

interface CreateTransactionInteractor {
    suspend fun createTransaction(transaction: CreateTransactionModel): BaseResult<TransactionModel>
    suspend fun getPendingTransactions(): List<PendingTransactionModel>
    suspend fun sendPendingTransactions(): Boolean
}

class CreateTransactionInteractorImpl(
    private val repository: CreateTransactionRepository,
    private val context: Context,
) : CreateTransactionInteractor {

    override suspend fun createTransaction(transaction: CreateTransactionModel): BaseResult<TransactionModel> {
        if (context.isNetworkAvailable()) {
            return create(transaction)
        } else {
            repository.addPendingTransaction(transaction.toEntity())
            return BaseResult.Success(null)
        }
    }

    override suspend fun getPendingTransactions(): List<PendingTransactionModel> {
        return repository.getPendingTransactions().map { it.toModel() }
    }

    override suspend fun sendPendingTransactions(): Boolean {
        var result = true

        if (context.isNetworkAvailable()) {
            getPendingTransactions().forEach {
                if (create(it.transactionModel) is BaseResult.Success) {
                    repository.removePendingTransaction(it.id)
                } else {
                    result = false
                }
            }
        } else {
            result = false
        }

        return result
    }

    private suspend fun create(transaction: CreateTransactionModel): BaseResult<TransactionModel> {
        val name = transaction.userInfo?.name
        val email = if (name != null) null else transaction.userInfo?.email
        val phone = if (name != null || email!= null) null else transaction.userInfo?.phone

        val recipient = UserInfoModel(
            name = name,
            email = email,
            phone = phone,
        )

        if (transaction.isProducerTransaction) {

            var qrFile: Pair<File?, MFile?> = null to null
            var qrLocation = transaction.location

            if (transaction.locationProvider == LocationProvider.QR && transaction.qr != null) {
                val qrGeoData = parseQrGeoData(transaction.qr)
                qrLocation = qrGeoData.location ?: transaction.location

                qrGeoData.geoJson?.let { geoJson ->
                    qrFile = writeQRDataToCacheFile(
                        context = context,
                        content = geoJson,
                    )
                }
            }

            val result = repository.createProducerTransaction(
                CreateProducerTransactionRequest(
                    isBuyingFromFarmer = transaction.isBuyingFromFarmer,
                    commodityId = transaction.commodity?.id,
                    volume = transaction.volume,
                    locationProvider = transaction.locationProvider?.providerName,
                    farmLatitude = qrLocation?.latitude,
                    farmLongitude = qrLocation?.longitude,
                    locationFile = qrFile.second ?: transaction.file,
                    transactionLatitude = transaction.creationLocation?.latitude,
                    transactionLongitude = transaction.creationLocation?.longitude,
                    recipient = recipient.toJsonArgs(),
                )
            )

            qrFile.first?.delete()

            return result
        } else {

            return repository.createDownstreamTransaction(
                CreateDownstreamTransactionRequest(
                    commodityId = transaction.commodity?.id,
                    volume = transaction.volume,
                    locationProvider = transaction.locationProvider?.providerName,
                    transactionLatitude = transaction.creationLocation?.latitude,
                    transactionLongitude = transaction.creationLocation?.longitude,
                    action = transaction.action?.actionName,
                    recipient = recipient.toJsonArgs(),
                )
            )
        }
    }
}
