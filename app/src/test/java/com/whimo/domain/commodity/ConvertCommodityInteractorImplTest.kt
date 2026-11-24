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
package com.whimo.domain.commodity

import com.whimo.data.base.common.BaseResult
import com.whimo.data.commodity.model.request.ConvertCommodityRequest
import com.whimo.data.commodity.repository.CommodityRepository
import com.whimo.data.commodity.repository.ConvertCommodityRepository
import com.whimo.domain.commodity.models.CommodityBalanceModel
import com.whimo.domain.commodity.models.CommodityGroupModel
import com.whimo.domain.commodity.models.CommodityModel
import com.whimo.domain.commodity.models.ConvertQuantityModel
import com.whimo.domain.commodity.models.ConvertRecipeModel
import com.whimo.domain.harvestseasons.models.HarvestSeasonModel
import com.whimo.domain.transactions.models.BaseModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertCommodityInteractorImplTest {

    @Test
    fun `convertCommodity sends selected harvest season id`() = runTest {
        val convertRepository = FakeConvertCommodityRepository()
        val interactor = ConvertCommodityInteractorImpl(
            repository = convertRepository,
            commodityRepository = FakeCommodityRepository(),
        )
        val input = ConvertQuantityModel(
            id = "input",
            commodity = commodity("input-commodity"),
            quantity = 1000f,
        )
        val output = ConvertQuantityModel(
            id = "output",
            commodity = commodity("output-commodity"),
            quantity = 850f,
        )
        val harvestSeason = HarvestSeasonModel(
            id = "season-id",
            name = "2026/27",
        )

        interactor.convertCommodity(
            recipeId = "recipe-id",
            inputs = listOf(input),
            outputs = listOf(output),
            harvestSeason = harvestSeason,
        )

        val request = convertRepository.lastRequest
        assertEquals("recipe-id", request?.recipe_id)
        assertEquals("season-id", request?.harvest_season_id)
        assertEquals("input-commodity", request?.input_overrides?.firstOrNull()?.commodity_id)
        assertEquals("output-commodity", request?.output_overrides?.firstOrNull()?.commodity_id)
    }

    private fun commodity(id: String): CommodityModel {
        return CommodityModel(
            id = id,
            code = id,
            name = id,
            unit = "kg",
            hasRecipe = true,
            group = null,
            balance = null,
        )
    }

    private class FakeConvertCommodityRepository : ConvertCommodityRepository {
        var lastRequest: ConvertCommodityRequest? = null

        override suspend fun getRecipes(
            search: String?,
            commodityId: String?,
            page: Int,
            pageSize: Int,
        ): BaseResult<List<ConvertRecipeModel>> {
            return BaseResult.Success(emptyList())
        }

        override suspend fun convertCommodity(
            request: ConvertCommodityRequest,
        ): BaseResult<BaseModel> {
            lastRequest = request
            return BaseResult.Success(BaseModel(success = true, message = ""))
        }
    }

    private class FakeCommodityRepository : CommodityRepository {
        override suspend fun getCommodities(
            search: String?,
            page: Int,
            pageSize: Int,
        ): BaseResult<List<CommodityGroupModel>> {
            return BaseResult.Success(emptyList())
        }

        override suspend fun getCommoditiesFromDB(): List<CommodityGroupModel> {
            return emptyList()
        }

        override suspend fun updateCommoditiesDB(items: List<CommodityGroupModel>?) = Unit

        override suspend fun getBalancesFromDB(
            search: String?,
            commodityGroupId: String?,
            commodityId: String?,
            harvestSeasonId: String?,
        ): List<CommodityBalanceModel> {
            return emptyList()
        }

        override suspend fun getBalances(
            search: String?,
            page: Int,
            pageSize: Int,
            commodityGroupId: String?,
            commodityId: String?,
            harvestSeasonId: String?,
        ): BaseResult<List<CommodityBalanceModel>> {
            return BaseResult.Success(emptyList())
        }
    }
}
