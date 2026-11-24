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
package com.whimo.data.transactions.model.mappers

import com.google.android.gms.maps.model.LatLng
import com.whimo.data.base.common.toDomain
import com.whimo.data.commodity.model.mappers.toDomain
import com.whimo.data.harvestseasons.model.mappers.toDomainOrNull
import com.whimo.data.transactions.model.entity.TransactionEntity
import com.whimo.data.transactions.model.response.BaseResponse
import com.whimo.data.transactions.model.response.ConversionTransactionData
import com.whimo.data.transactions.model.response.TransactionData
import com.whimo.data.transactions.model.response.TransactionDetailsResponse
import com.whimo.data.transactions.model.response.TransactionListLocationSummaryResponse
import com.whimo.data.transactions.model.response.TransactionsResponse
import com.whimo.data.transactions.model.response.User
import com.whimo.domain.commodity.models.CommodityModel
import com.whimo.domain.common.PaginationModel
import com.whimo.domain.createtransaction.models.LocationProvider
import com.whimo.domain.transactions.models.BaseModel
import com.whimo.domain.transactions.models.ConversionTransactionItemModel
import com.whimo.domain.transactions.models.GadgetTypes
import com.whimo.domain.transactions.models.TraceabilityStatus
import com.whimo.domain.transactions.models.TransactionAction
import com.whimo.domain.transactions.models.TransactionListDownloadSummaryModel
import com.whimo.domain.transactions.models.TransactionModel
import com.whimo.domain.transactions.models.TransactionStatus
import com.whimo.domain.transactions.models.TransactionType
import com.whimo.domain.transactions.models.UserModel
import com.whimo.extensions.toLocalDateTime
import com.whimo.utils.gson
import kotlin.math.absoluteValue

fun BaseResponse.toDomain(): BaseModel {
    return BaseModel(
        success = success,
        message = message,
    )
}

fun TransactionsResponse.toDomain(): Pair<PaginationModel, List<TransactionModel>> {
    return pagination.toDomain() to data.map { it.toDomain() }
}

fun TransactionDetailsResponse.toDomain(): TransactionModel {
    return data.toDomain()
}

fun TransactionListLocationSummaryResponse.toDomain(): TransactionListDownloadSummaryModel {
    return TransactionListDownloadSummaryModel(
        fullTraceabilityFarms = data?.full_traceability_farms ?: 0,
        manuallyAddedFarms = data?.manually_added_farms ?: 0,
        farmsWithoutGeolocation = data?.farms_without_geolocation ?: 0,
        traders = data?.traders ?: 0,
    )
}

fun TransactionData.toDomain(): TransactionModel {
    val type = TransactionType.entries.find { it.typeName == type }
    val createdDate = created_at.toLocalDateTime()
    val inputs = inputs.orEmpty().mapNotNull { it.toDomain() }
    val outputs = outputs.orEmpty().mapNotNull { it.toDomain() }
    val fallbackCommodity = commodity?.toDomain()
        ?: inputs.firstOrNull()?.commodity
        ?: outputs.firstOrNull()?.commodity
        ?: emptyCommodity()
    val fallbackVolume = volume
        ?: signed_volume?.absoluteValue
        ?: inputs.firstOrNull()?.volume
        ?: outputs.firstOrNull()?.volume
        ?: 0f
    val isConversion = type == TransactionType.Conversion || inputs.isNotEmpty() || outputs.isNotEmpty()

    val status = if (is_automatic == true) {
        TransactionStatus.Automatic
    } else if (type == TransactionType.Producer) {
        TransactionStatus.Recorded
    } else {
        status?.let { statusName -> TransactionStatus.entries.find { it.statusName == statusName } }
    }

    return TransactionModel(
        id = if (isConversion) group_id ?: id.orEmpty() else id.orEmpty(),
        createdDate = createdDate,
        updatedDate = updated_at?.toLocalDateTime() ?: createdDate,
        expiresDate = expires_at?.toLocalDateTime(),
        type = type,
        status = status,
        action = action?.let { actionName -> TransactionAction.entries.find { it.actionName == actionName } },
        locationProvider = LocationProvider.entries.find { it.providerName == location },
        location = getTransactionLocation(),
        commodity = fallbackCommodity,
        volume = fallbackVolume,
        traceability = TraceabilityStatus.entries.find { it.statusName == traceability },
        seller = seller?.toDomain(),
        buyer = buyer?.toDomain(),
        isBuyingFromFarmer = is_buying_from_farmer == true,
        isAutomatic = is_automatic == true,
        harvestSeason = harvest_season?.toDomainOrNull(),
        createdById = created_by_id,
        groupId = group_id,
        signedVolume = signed_volume,
        conversionInputs = inputs,
        conversionOutputs = outputs,
    )
}

fun ConversionTransactionData.toDomain(): ConversionTransactionItemModel? {
    val commodity = commodity?.toDomain() ?: return null
    val volume = volume ?: signed_volume?.absoluteValue ?: return null

    return ConversionTransactionItemModel(
        id = transaction_id ?: id,
        commodity = commodity,
        volume = volume,
        signedVolume = signed_volume,
    )
}

private fun emptyCommodity(): CommodityModel {
    return CommodityModel(
        id = "",
        code = "",
        name = "",
        unit = "",
        hasRecipe = false,
        group = null,
        balance = null,
    )
}

private fun TransactionData.getTransactionLocation(): LatLng? {
    val latitudeValue = transaction_latitude ?: latitude ?: farm_latitude
    val longitudeValue = transaction_longitude ?: longitude ?: farm_longitude

    return if (latitudeValue != null && longitudeValue != null) {
        LatLng(latitudeValue, longitudeValue)
    } else {
        null
    }
}

fun User.toDomain(): UserModel {
    var email: String? = null
    var phone: String? = null

    gadgets?.forEach {
        if (it.is_verified) {
            when (it.type) {
                GadgetTypes.Email.gadgetName -> email = it.identifier
                GadgetTypes.Phone.gadgetName -> phone = it.identifier
            }
        }
    }

    return UserModel(
        id = id,
        username = username,
        email = email,
        phone = phone,
    )
}

fun TransactionModel.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        status = status?.statusName,
        action = action?.actionName,
        dataJson = gson.toJson(this),
    )
}

fun TransactionEntity.toDomain(): TransactionModel {
    return gson.fromJson(dataJson, TransactionModel::class.java)
}
