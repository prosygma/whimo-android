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
package com.whimo.presentation.balances.convert

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.whimo.base.CoreViewModel
import com.whimo.data.base.common.BaseResult
import com.whimo.domain.commodity.CommodityInteractor
import com.whimo.domain.commodity.models.CommodityBalanceFilter
import com.whimo.domain.commodity.models.CommodityBalanceModel
import com.whimo.domain.commodity.models.CommodityModel
import com.whimo.domain.commodity.models.ConvertQuantityModel
import com.whimo.domain.commodity.models.ConvertRecipeModel
import com.whimo.domain.harvestseasons.models.HarvestSeasonModel
import com.whimo.network.ErrorHandler
import com.whimo.presentation.balances.ConvertCommodityArgs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConvertCommoditySeasonBalanceTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val season = HarvestSeasonModel("selected-season", "2025/26")
    private val commodity = CommodityModel("beans", "0901", "Green bean", "kg", true, null, 9999f)
    private val input = ConvertQuantityModel("input", commodity, 1000f)
    private val recipe = ConvertRecipeModel("recipe", "Roasting", listOf(input), listOf(input.copy(id = "output")))
    private lateinit var balances: CommodityInteractor
    private lateinit var viewModel: ConvertCommodityViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        balances = mockk()
        coEvery { balances.getBalancesFromDB(any()) } returns emptyList()
        viewModel = ConvertCommodityViewModel(
            interactor = mockk(),
            errorHandler = ErrorHandler(mockk(relaxed = true)),
            commodityInteractor = balances,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uses balance matching both commodity and season instead of aggregate`() = runTest {
        coEvery { balances.getBalances(any()) } returns BaseResult.Success(listOf(
            balance(400f, season = HarvestSeasonModel("other-season")),
            balance(500f, commodity = commodity.copy(id = "other-commodity")),
            balance(60f),
        ))
        create()
        advanceUntilIdle()

        assertEquals(season, binding().harvestSeason)
        assertEquals(mapOf(commodity.id to 60f), binding().seasonBalances)
        coVerify(exactly = 1) {
            balances.getBalances(CommodityBalanceFilter(commodityId = commodity.id, harvestSeason = season))
        }
    }

    @Test
    fun `loads each distinct input commodity and never loads output only commodities`() = runTest {
        val second = commodity.copy(id = "second", unit = "t")
        coEvery { balances.getBalances(any()) } answers {
            val filter = firstArg<CommodityBalanceFilter>()
            BaseResult.Success(listOf(balance(
                volume = if (filter.commodityId == commodity.id) 50f else 20f,
                commodity = if (filter.commodityId == commodity.id) commodity else second,
            )))
        }
        create(recipe.copy(
            inputs = listOf(input, input.copy(id = "repeat"), input.copy(id = "second-input", commodity = second)),
            outputs = listOf(input.copy(id = "output", commodity = commodity.copy(id = "output-only"))),
        ))
        advanceUntilIdle()

        assertEquals(mapOf(commodity.id to 50f, second.id to 20f), binding().seasonBalances)
        coVerify(exactly = 2) { balances.getBalances(any()) }
    }

    @Test
    fun `zero balance remains known`() = runTest {
        coEvery { balances.getBalances(any()) } returns BaseResult.Success(listOf(balance(0f)))
        create()
        advanceUntilIdle()
        assertEquals(mapOf(commodity.id to 0f), binding().seasonBalances)
    }

    @Test
    fun `missing or mismatched balances remain unknown`() = runTest {
        coEvery { balances.getBalances(any()) } returns BaseResult.Success(listOf(
            balance(100f, season = null),
            balance(200f, season = HarvestSeasonModel("other")),
        ))
        create()
        advanceUntilIdle()
        assertEquals(emptyMap<String, Float>(), binding().seasonBalances)
    }

    @Test
    fun `failed balance request keeps season and does not invent a balance`() = runTest {
        coEvery { balances.getBalances(any()) } returns BaseResult.Error(IllegalStateException("offline"))
        create()
        advanceUntilIdle()
        assertEquals(season, binding().harvestSeason)
        assertEquals(emptyMap<String, Float>(), binding().seasonBalances)
        assertEquals(recipe.inputs, binding().inputs)
    }

    @Test
    fun `missing season skips season balance requests`() = runTest {
        create(season = null)
        advanceUntilIdle()
        assertNull(binding().harvestSeason)
        assertEquals(emptyMap<String, Float>(), binding().seasonBalances)
        coVerify(exactly = 0) {
            balances.getBalances(any())
            balances.getBalancesFromDB(any())
        }
    }

    @Test
    fun `recreating screen cancels old balance load and clears old season data`() = runTest {
        coEvery { balances.getBalances(any()) } coAnswers {
            try {
                delay(1000)
            } catch (_: CancellationException) {
                // Simulate the repository returning its cache after a cancelled request.
            }
            BaseResult.Success(listOf(balance(100f)))
        }
        create()
        runCurrent()
        create(season = null)
        advanceUntilIdle()
        assertNull(binding().harvestSeason)
        assertEquals(emptyMap<String, Float>(), binding().seasonBalances)
    }

    @Test
    fun `loading balance does not overwrite edited quantity`() = runTest {
        coEvery { balances.getBalances(any()) } coAnswers {
            delay(1000)
            BaseResult.Success(listOf(balance(60f)))
        }
        create()
        runCurrent()
        viewModel.handleEvents(ConvertCommodityContract.Event.OnQuantityChange(input, "25"))
        advanceUntilIdle()
        assertEquals(25f, binding().inputs!!.first().quantity)
        assertEquals(mapOf(commodity.id to 60f), binding().seasonBalances)
    }

    @Test
    fun `shows all cached inputs before network finishes then refreshes balances`() = runTest {
        val second = commodity.copy(id = "second")
        coEvery { balances.getBalancesFromDB(any()) } answers {
            val filter = firstArg<CommodityBalanceFilter>()
            listOf(balance(
                volume = if (filter.commodityId == commodity.id) 50f else 20f,
                commodity = if (filter.commodityId == commodity.id) commodity else second,
            ))
        }
        coEvery { balances.getBalances(any()) } coAnswers {
            val filter = firstArg<CommodityBalanceFilter>()
            delay(1000)
            BaseResult.Success(listOf(balance(
                volume = if (filter.commodityId == commodity.id) 60f else 30f,
                commodity = if (filter.commodityId == commodity.id) commodity else second,
            )))
        }
        create(recipe.copy(inputs = listOf(input, input.copy(id = "second-input", commodity = second))))
        runCurrent()

        assertEquals(mapOf(commodity.id to 50f, second.id to 20f), binding().seasonBalances)
        coVerify(exactly = 1) {
            balances.getBalancesFromDB(CommodityBalanceFilter(commodityId = commodity.id, harvestSeason = season))
            balances.getBalancesFromDB(CommodityBalanceFilter(commodityId = second.id, harvestSeason = season))
        }
        advanceUntilIdle()
        assertEquals(mapOf(commodity.id to 60f, second.id to 30f), binding().seasonBalances)
    }

    @Test
    fun `offline error retains cached zero balance and ignores other commodities and seasons`() = runTest {
        coEvery { balances.getBalancesFromDB(any()) } returns listOf(
            balance(100f, season = HarvestSeasonModel("other-season")),
            balance(200f, commodity = commodity.copy(id = "other-commodity")),
            balance(0f),
        )
        coEvery { balances.getBalances(any()) } coAnswers {
            delay(1000)
            BaseResult.Error(IllegalStateException("offline"))
        }
        create()
        runCurrent()
        assertEquals(mapOf(commodity.id to 0f), binding().seasonBalances)
        advanceUntilIdle()
        assertEquals(mapOf(commodity.id to 0f), binding().seasonBalances)
    }

    private fun create(recipe: ConvertRecipeModel = this.recipe, season: HarvestSeasonModel? = this.season) {
        viewModel.handleEvents(ConvertCommodityContract.Event.OnCreate(ConvertCommodityArgs(recipe, season)))
    }

    private fun balance(
        volume: Float,
        commodity: CommodityModel = this.commodity,
        season: HarvestSeasonModel? = this.season,
    ) = CommodityBalanceModel("balance", volume, commodity, season, true)

    private fun binding(): ConvertCommodityContract.Binding {
        val field = CoreViewModel::class.java.getDeclaredField("binding")
        field.isAccessible = true
        return field.get(viewModel) as ConvertCommodityContract.Binding
    }
}
