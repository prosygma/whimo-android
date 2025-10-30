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
package com.whimo.data.notifications.model.mappers

import com.whimo.data.base.common.toDomain
import com.whimo.data.notifications.model.entity.NotificationEntity
import com.whimo.data.notifications.model.response.NotificationData
import com.whimo.data.notifications.model.response.NotificationsResponse
import com.whimo.data.notifications.model.response.ShortTransactionData
import com.whimo.data.transactions.model.mappers.toDomain
import com.whimo.domain.common.PaginationModel
import com.whimo.domain.notifications.models.NotificationModel
import com.whimo.domain.notifications.models.NotificationStatus
import com.whimo.domain.notifications.models.NotificationType
import com.whimo.domain.transactions.models.TransactionModel
import com.whimo.extensions.toLocalDateTime
import com.whimo.utils.gson

fun NotificationsResponse.toDomain(): Pair<PaginationModel, List<NotificationModel>> {
    return pagination.toDomain() to data.map { it.toDomain() }
}

fun NotificationData.toDomain(): NotificationModel {
    return NotificationModel(
        id = id,
        createdDate = created_at.toLocalDateTime(),
        transactionId = data?.transaction_id,
        transaction = data?.toDomain(),
        type = NotificationType.entries.find { it.typeName == type },
        status = NotificationStatus.entries.find { it.statusName == status },
        receivedBy = received_by?.toDomain(),
        createdBy = created_by?.toDomain(),
    )
}

fun ShortTransactionData.toDomain(): TransactionModel? {
    return transaction?.toDomain()
}

fun NotificationModel.toEntity(): NotificationEntity {
    return NotificationEntity(
        id = id,
        type = type?.typeName,
        dataJson = gson.toJson(this),
    )
}

fun NotificationEntity.toDomain(): NotificationModel {
    return gson.fromJson(dataJson, NotificationModel::class.java)
}