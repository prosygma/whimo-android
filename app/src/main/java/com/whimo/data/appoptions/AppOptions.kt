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
package com.whimo.data.appoptions

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.whimo.data.base.common.onSuccess
import com.whimo.network.handleResponse
import retrofit2.Response
import retrofit2.http.GET

/** Geodata choices set in the admin (Settings > Options). */
data class AppOptions(
    val askOnPlot: Boolean = true,
    val allowGpsPoint: Boolean = true,
    val allowCiccQr: Boolean = false,
)

data class AppOptionsResponse(
    val success: Boolean?,
    val data: AppOptionsData?,
)

data class AppOptionsData(
    val ask_on_plot: Boolean?,
    val allow_gps_point: Boolean?,
    val allow_cicc_qr: Boolean?,
)

interface AppOptionsService {
    @GET("app-options/")
    suspend fun getAppOptions(): Response<AppOptionsResponse>
}

interface AppOptionsRepository {
    /** Last options received, or the defaults (today's behaviour) before the first sync. */
    fun current(): AppOptions

    suspend fun sync()
}

class AppOptionsRepositoryImpl(
    private val service: AppOptionsService,
    context: Context,
) : AppOptionsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun current(): AppOptions {
        val defaults = AppOptions()
        return AppOptions(
            askOnPlot = prefs.getBoolean(KEY_ASK_ON_PLOT, defaults.askOnPlot),
            allowGpsPoint = prefs.getBoolean(KEY_ALLOW_GPS_POINT, defaults.allowGpsPoint),
            allowCiccQr = prefs.getBoolean(KEY_ALLOW_CICC_QR, defaults.allowCiccQr),
        )
    }

    override suspend fun sync() {
        handleResponse { service.getAppOptions() }.onSuccess { response ->
            val data = response?.data ?: return@onSuccess
            val defaults = AppOptions()
            prefs.edit {
                putBoolean(KEY_ASK_ON_PLOT, data.ask_on_plot ?: defaults.askOnPlot)
                putBoolean(KEY_ALLOW_GPS_POINT, data.allow_gps_point ?: defaults.allowGpsPoint)
                putBoolean(KEY_ALLOW_CICC_QR, data.allow_cicc_qr ?: defaults.allowCiccQr)
            }
        }
    }

    private companion object {
        const val PREFS = "app_options"
        const val KEY_ASK_ON_PLOT = "ask_on_plot"
        const val KEY_ALLOW_GPS_POINT = "allow_gps_point"
        const val KEY_ALLOW_CICC_QR = "allow_cicc_qr"
    }
}
