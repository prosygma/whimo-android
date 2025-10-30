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
package com.whimo.services

import android.Manifest
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.whimo.R
import com.whimo.data.base.common.onError
import com.whimo.data.base.common.onSuccess
import com.whimo.data.notifications.model.mappers.toDomain
import com.whimo.data.notifications.model.response.NotificationData
import com.whimo.domain.notifications.PushNotificationsInteractor
import com.whimo.domain.notifications.models.NotificationType
import com.whimo.domain.transactions.TransactionDetailsInteractor
import com.whimo.domain.transactions.models.TransactionModel
import com.whimo.domain.transactions.models.getCommodityFullText
import com.whimo.presentation.main.MainActivity
import com.whimo.providers.ResourceProvider
import com.whimo.providers.SharedPreferencesProvider
import com.whimo.utils.gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val interactor: PushNotificationsInteractor by inject()
    private val transactionInteractor: TransactionDetailsInteractor by inject()
    private val resourceProvider: ResourceProvider by inject()
    private val sharedPreferencesProvider: SharedPreferencesProvider by inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        CoroutineScope(Dispatchers.IO).launch {
            interactor.addPushNotificationDevice(token)
                .onSuccess {
                    sharedPreferencesProvider.setFCMToken(token)
                }
                .onError {
                    it.printStackTrace()
                }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data["data"] ?: return

        val notificationData = gson.fromJson(data, NotificationData::class.java)
        val notificationModel = notificationData.toDomain()

        if (sharedPreferencesProvider.isNotificationsAllowed() && notificationModel.type != null) {
            val title = getNotificationTitle(notificationModel.type)

            if (notificationModel.transactionId != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    transactionInteractor.getTransactionDetails(notificationModel.transactionId)
                        .onSuccess {
                            showNotification(title, it)
                        }
                        .onError {
                            showNotification(title, notificationModel.transaction)
                        }
                }

            } else {
                showNotification(title, notificationModel.transaction)
            }
        }
    }

    private fun showNotification(title: String, transactionModel: TransactionModel?) {
        val message = transactionModel?.getCommodityFullText()
        showNotification(title, message)
    }

    private fun showNotification(title: String, message: String?) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val intent = Intent(applicationContext, MainActivity::class.java)

        val action = TaskStackBuilder.create(applicationContext)
            .addNextIntent(intent)
            .getPendingIntent(
                Random.nextInt(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val builder = NotificationCompat.Builder(this, PUSH_NOTIFICATIONS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentIntent(action)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (!message.isNullOrEmpty()) {
            builder.setContentText(message)
        }

        val notificationManager = NotificationManagerCompat.from(this)
        val notificationId = Random.nextInt()

        notificationManager.notify(notificationId, builder.build())
    }

    private fun getNotificationTitle(type: NotificationType): String {
        return when (type) {
            NotificationType.TransactionPending -> resourceProvider.getString(R.string.transaction_approval_request)
            NotificationType.TransactionAccepted -> resourceProvider.getString(R.string.transaction_accepted)
            NotificationType.TransactionRejected -> resourceProvider.getString(R.string.transaction_rejected)
            NotificationType.TransactionExpired -> resourceProvider.getString(R.string.transaction_expired)
            NotificationType.GeoDataMissing -> resourceProvider.getString(R.string.missing_location_request)
            NotificationType.GeoDataUpdated -> resourceProvider.getString(R.string.location_updated)
        }
    }

    companion object {
        const val PUSH_NOTIFICATIONS_CHANNEL_ID = "whimo_notification_channel"
        const val PUSH_NOTIFICATIONS_CHANNEL_NAME = "Push notifications"
    }
}