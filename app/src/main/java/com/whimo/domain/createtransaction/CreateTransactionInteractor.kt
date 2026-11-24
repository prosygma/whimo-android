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
import android.net.Uri
import android.util.Log
import com.whimo.data.base.common.BaseResult
import com.whimo.data.createtransaction.model.mappers.toEntity
import com.whimo.data.createtransaction.model.mappers.toModel
import com.whimo.data.createtransaction.model.request.CreateDownstreamTransactionRequest
import com.whimo.data.createtransaction.model.request.CreateProducerTransactionRequest
import com.whimo.data.createtransaction.repository.CreateTransactionRepository
import com.whimo.domain.createtransaction.models.CreateTransactionModel
import com.whimo.domain.createtransaction.models.LocationProvider
import com.whimo.domain.createtransaction.models.MFile
import com.whimo.domain.createtransaction.models.PendingTransactionModel
import com.whimo.domain.createtransaction.models.PendingTransactionsSyncProgress
import com.whimo.domain.createtransaction.models.UserInfoModel
import com.whimo.domain.transactions.models.TransactionModel
import com.whimo.extensions.isNetworkAvailable
import com.whimo.utils.parseQrGeoData
import com.whimo.utils.toJsonArgs
import com.whimo.utils.writeQRDataToCacheFile
import java.io.File
import java.io.IOException
import java.util.UUID

interface CreateTransactionInteractor {
    suspend fun createTransaction(transaction: CreateTransactionModel): BaseResult<TransactionModel>
    suspend fun getPendingTransactions(): List<PendingTransactionModel>
    suspend fun clearPendingTransactions()
    suspend fun sendPendingTransactions(): Boolean
    suspend fun sendPendingTransactions(
        onProgress: suspend (PendingTransactionsSyncProgress) -> Unit,
    ): Boolean {
        return sendPendingTransactions()
    }
}

class CreateTransactionInteractorImpl(
    private val repository: CreateTransactionRepository,
    private val context: Context,
) : CreateTransactionInteractor {

    override suspend fun createTransaction(transaction: CreateTransactionModel): BaseResult<TransactionModel> {
        validateTransaction(transaction)?.let { return BaseResult.Error(it) }

        if (context.isNetworkAvailable()) {
            return create(transaction)
        } else {
            var localFile: MFile? = null
            return try {
                localFile = transaction.file?.let(::copyPendingAttachment)
                repository.addPendingTransaction(transaction.copy(file = localFile).toEntity())
                BaseResult.Success(null)
            } catch (e: Exception) {
                deletePendingAttachment(localFile)
                BaseResult.Error(e)
            }
        }
    }

    override suspend fun getPendingTransactions(): List<PendingTransactionModel> {
        return repository.getPendingTransactions().mapNotNull { entity ->
            try {
                entity.toModel()
            } catch (e: Exception) {
                // Keep the original database row so a damaged legacy attachment is never discarded.
                Log.e("PendingTransactions", "Cannot read pending transaction ${entity.id}", e)
                null
            }
        }
    }

    override suspend fun clearPendingTransactions() {
        repository.clearPendingTransactions()
        pendingAttachmentsDir().listFiles()?.forEach(File::delete)
    }

    override suspend fun sendPendingTransactions(): Boolean {
        return sendPendingTransactions {}
    }

    override suspend fun sendPendingTransactions(
        onProgress: suspend (PendingTransactionsSyncProgress) -> Unit,
    ): Boolean {
        var result = true

        if (context.isNetworkAvailable()) {
            val pendingTransactions = getPendingTransactions()
            if (pendingTransactions.size != repository.getPendingTransactions().size) result = false
            val totalTransactions = pendingTransactions.size

            if (totalTransactions > 0) {
                onProgress(
                    PendingTransactionsSyncProgress(
                        processedTransactions = 0,
                        totalTransactions = totalTransactions,
                    )
                )
            }

            pendingTransactions.forEachIndexed { index, pendingTransaction ->
                val createResult = validateTransaction(pendingTransaction.transactionModel)
                    ?.let { error -> BaseResult.Error(error) }
                    ?: create(pendingTransaction.transactionModel)

                if (createResult is BaseResult.Success) {
                    repository.removePendingTransaction(pendingTransaction.id)
                    deletePendingAttachment(pendingTransaction.transactionModel.file)
                } else {
                    result = false
                }

                onProgress(
                    PendingTransactionsSyncProgress(
                        processedTransactions = index + 1,
                        totalTransactions = totalTransactions,
                    )
                )
            }
        } else {
            result = false
        }

        return result
    }

    private fun pendingAttachmentsDir(): File = File(context.filesDir, "pending-transaction-files")

    private fun copyPendingAttachment(file: MFile): MFile {
        val source = file.uri ?: throw IOException("Attachment URI is missing")
        val directory = pendingAttachmentsDir()
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Cannot create pending attachment directory")
        }
        val target = File(directory, UUID.randomUUID().toString())
        try {
            val input = context.contentResolver.openInputStream(source)
                ?: throw IOException("Cannot open attachment: $source")
            input.use { stream -> target.outputStream().use { output -> stream.copyTo(output) } }
            return file.copy(
                uri = Uri.fromFile(target),
                size = target.length(),
                mimeType = file.mimeType ?: context.contentResolver.getType(source),
            )
        } catch (e: Exception) {
            target.delete()
            throw e
        }
    }

    private fun deletePendingAttachment(file: MFile?) {
        val uri = file?.uri ?: return
        if (uri.scheme != "file") return
        val path = uri.path ?: return
        val local = File(path)
        if (local.parentFile == pendingAttachmentsDir()) local.delete()
    }

    private suspend fun create(transaction: CreateTransactionModel): BaseResult<TransactionModel> {
        val name = transaction.userInfo?.name
        val email = if (name != null) null else transaction.userInfo?.email
        val phone = if (name != null || email != null) null else transaction.userInfo?.phone

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
                    harvestSeasonId = transaction.harvestSeason?.id,
                )
            )

            qrFile.first?.delete()

            return result
        } else {

            return repository.createDownstreamTransaction(
                CreateDownstreamTransactionRequest(
                    commodityId = transaction.commodity?.id,
                    volume = transaction.volume,
                    transactionLatitude = transaction.creationLocation?.latitude,
                    transactionLongitude = transaction.creationLocation?.longitude,
                    action = transaction.action?.actionName,
                    recipient = recipient.toJsonArgs(),
                    harvestSeasonId = transaction.harvestSeason?.id,
                )
            )
        }
    }

    private fun validateTransaction(transaction: CreateTransactionModel): Throwable? {
        if (transaction.commodity?.id.isNullOrBlank()) {
            return IllegalArgumentException("commodity_id is required for transactions")
        }

        if (transaction.volume == null) {
            return IllegalArgumentException("volume is required for transactions")
        }

        if (!transaction.isProducerTransaction && transaction.action == null) {
            return IllegalArgumentException("action is required for downstream transactions")
        }

        if (!transaction.isProducerTransaction && transaction.harvestSeason?.id.isNullOrBlank()) {
            return IllegalArgumentException("harvest_season_id is required for downstream transactions")
        }

        return null
    }
}
