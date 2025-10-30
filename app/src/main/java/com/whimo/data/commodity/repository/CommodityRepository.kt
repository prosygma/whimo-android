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
package com.whimo.data.commodity.repository

import com.whimo.data.base.common.BaseResult
import com.whimo.data.commodity.model.mappers.toDomain
import com.whimo.data.commodity.model.mappers.toEntity
import com.whimo.data.commodity.service.CommodityGroupsDao
import com.whimo.data.commodity.service.CommodityService
import com.whimo.domain.commodity.models.CommodityBalanceModel
import com.whimo.domain.commodity.models.CommodityGroupModel
import com.whimo.network.handleResponse
import com.whimo.network.mapResult

interface CommodityRepository {
    suspend fun getCommodities(
        search: String?,
        page: Int,
        pageSize: Int,
    ): BaseResult<List<CommodityGroupModel>>

    suspend fun getCommoditiesFromDB(): List<CommodityGroupModel>

    suspend fun updateCommoditiesDB(items: List<CommodityGroupModel>?)

    suspend fun getCommoditiesBalances(
        search: String?,
        page: Int,
        pageSize: Int,
        groupId: String?,
        commodityId: String?,
    ): BaseResult<List<CommodityBalanceModel>>
}

class CommodityRepositoryImpl(
    private val service: CommodityService,
    private val dao: CommodityGroupsDao,
) : CommodityRepository {

    override suspend fun getCommodities(
        search: String?,
        page: Int,
        pageSize: Int,
    ): BaseResult<List<CommodityGroupModel>> {
        return handleResponse {
            service.getCommodities(
                search = search,
                page = page,
                pageSize = pageSize,
            )
        }.mapResult { it?.toDomain() }
    }

    override suspend fun getCommoditiesFromDB(): List<CommodityGroupModel> {
        return dao.getAll().map { it.toDomain() }
    }

    override suspend fun updateCommoditiesDB(items: List<CommodityGroupModel>?) {
        dao.clearAll()
        if (items != null) {
            dao.insertAll(items.map { it.toEntity() })
        }
    }

    override suspend fun getCommoditiesBalances(
        search: String?,
        page: Int,
        pageSize: Int,
        groupId: String?,
        commodityId: String?
    ): BaseResult<List<CommodityBalanceModel>> {
        return handleResponse {
            service.getCommoditiesBalances(
                search = search,
                page = page,
                pageSize = pageSize,
                groupId = groupId,
                commodityId = commodityId,
            )
        }.mapResult { it?.toDomain() }
    }
}