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
package com.whimo.data.transactions.service

import com.whimo.data.transactions.model.request.UpdateTransactionStatusRequest
import com.whimo.data.transactions.model.response.BaseResponse
import com.whimo.data.transactions.model.response.TransactionDetailsResponse
import com.whimo.data.transactions.model.response.TransactionListLocationSummaryResponse
import com.whimo.data.transactions.model.response.TransactionTraceabilityCountsResponse
import com.whimo.data.transactions.model.response.TransactionsResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface TransactionsService {
    @GET("transactions/")
    suspend fun getTransactions(
        @Query("search") search: String?,
        @Query("page") page: Int?,
        @Query("page_size") pageSize: Int?,
        @Query("status") status: String?,
        @Query("action") action: String?,
        @Query("type") type: String?,
        @Query("group_conversions") groupConversions: Boolean?,
        @Query("created_at_from") createdAtFrom: String?,
        @Query("created_at_to") createdAtTo: String?,
        @Query("commodity_group_id") commodityGroupId: String?,
        @Query("commodity_id") commodityId: String?,
        @Query("buyer_id") buyerId: String?,
        @Query("harvest_season_id") harvestSeasonId: String?,
    ): Response<TransactionsResponse>

    @Streaming
    @GET("transactions/download/csv/")
    suspend fun downloadTransactionListCSV(
        @Query("search") search: String?,
        @Query("include_chain") includeChain: Boolean?,
        @Query("status") status: String?,
        @Query("action") action: String?,
        @Query("type") type: String?,
        @Query("group_conversions") groupConversions: Boolean?,
        @Query("created_at_from") createdAtFrom: String?,
        @Query("created_at_to") createdAtTo: String?,
        @Query("commodity_group_id") commodityGroupId: String?,
        @Query("commodity_id") commodityId: String?,
        @Query("buyer_id") buyerId: String?,
        @Query("harvest_season_id") harvestSeasonId: String?,
    ): Response<ResponseBody>

    @Streaming
    @GET("transactions/download/bundle/")
    suspend fun downloadTransactionListLocationBundle(
        @Query("search") search: String?,
        @Query("status") status: String?,
        @Query("action") action: String?,
        @Query("type") type: String?,
        @Query("group_conversions") groupConversions: Boolean?,
        @Query("created_at_from") createdAtFrom: String?,
        @Query("created_at_to") createdAtTo: String?,
        @Query("commodity_group_id") commodityGroupId: String?,
        @Query("commodity_id") commodityId: String?,
        @Query("buyer_id") buyerId: String?,
        @Query("harvest_season_id") harvestSeasonId: String?,
    ): Response<ResponseBody>

    @GET("transactions/download/summary/")
    suspend fun getTransactionListLocationSummary(
        @Query("search") search: String?,
        @Query("status") status: String?,
        @Query("action") action: String?,
        @Query("type") type: String?,
        @Query("group_conversions") groupConversions: Boolean?,
        @Query("created_at_from") createdAtFrom: String?,
        @Query("created_at_to") createdAtTo: String?,
        @Query("commodity_group_id") commodityGroupId: String?,
        @Query("commodity_id") commodityId: String?,
        @Query("buyer_id") buyerId: String?,
        @Query("harvest_season_id") harvestSeasonId: String?,
    ): Response<TransactionListLocationSummaryResponse>

    @GET("transactions/{transaction_id}/")
    suspend fun getTransactionDetails(
        @Path("transaction_id") transactionId: String,
    ): Response<TransactionDetailsResponse>

    @GET("transactions/conversion/{group_id}/")
    suspend fun getConversionTransactionDetails(
        @Path("group_id") groupId: String,
    ): Response<TransactionDetailsResponse>

    @GET("transactions/{transaction_id}/traceability-counts/")
    suspend fun getTransactionTraceabilityCounts(
        @Path("transaction_id") transactionId: String,
    ): Response<TransactionTraceabilityCountsResponse>

    @PATCH("transactions/{transaction_id}/status/")
    suspend fun updateTransactionStatus(
        @Path("transaction_id") transactionId: String,
        @Body request: UpdateTransactionStatusRequest,
    ): Response<BaseResponse>

    @POST("transactions/{transaction_id}/notification/resend/")
    suspend fun resendNotification(
        @Path("transaction_id") transactionId: String,
    ): Response<BaseResponse>
}
