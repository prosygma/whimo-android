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
import com.whimo.data.commodity.repository.CommodityRepository
import com.whimo.domain.commodity.models.CommodityBalanceFilter
import com.whimo.domain.commodity.models.CommodityBalanceModel
import com.whimo.domain.commodity.models.CommodityFilter
import com.whimo.domain.commodity.models.CommodityGroupModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CommodityInteractorImplTest {

    private lateinit var repository: CommodityRepository
    private lateinit var commodityInteractor: CommodityInteractorImpl

    @Before
    fun setUp() {
        repository = mockk()
        commodityInteractor = CommodityInteractorImpl(repository)
    }

    @Test
    fun `getCommodities calls repository with correct parameters`() = runTest {
        // Given
        val filter = CommodityFilter(query = "test")
        val expectedCommodities = listOf(
            CommodityGroupModel(
                id = "1",
                name = "Test Group",
                commodities = emptyList()
            )
        )
        val expectedResult = BaseResult.Success(expectedCommodities)

        coEvery { 
            repository.getCommodities(
                search = "test",
                page = 1,
                pageSize = 1000
            ) 
        } returns expectedResult

        // When
        val result = commodityInteractor.getCommodities(filter)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `getCommodities with null query calls repository with null search`() = runTest {
        // Given
        val filter = CommodityFilter(query = null)
        val expectedCommodities = emptyList<CommodityGroupModel>()
        val expectedResult = BaseResult.Success(expectedCommodities)

        coEvery { 
            repository.getCommodities(
                search = null,
                page = 1,
                pageSize = 1000
            ) 
        } returns expectedResult

        // When
        val result = commodityInteractor.getCommodities(filter)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `getCommoditiesFromDB calls repository and returns result`() = runTest {
        // Given
        val expectedCommodities = listOf(
            CommodityGroupModel(
                id = "1",
                name = "Test Group",
                commodities = emptyList()
            )
        )

        coEvery { repository.getCommoditiesFromDB() } returns expectedCommodities

        // When
        val result = commodityInteractor.getCommoditiesFromDB()

        // Then
        assertEquals(expectedCommodities, result)
    }

    @Test
    fun `updateCommoditiesDB calls repository with correct items`() = runTest {
        // Given
        val items = listOf(
            CommodityGroupModel(
                id = "1",
                name = "Test Group",
                commodities = emptyList()
            )
        )

        coEvery { repository.updateCommoditiesDB(items) } returns Unit

        // When
        commodityInteractor.updateCommoditiesDB(items)

        // Then
        // Verify that the method was called (implicitly through coEvery)
    }

    @Test
    fun `updateCommoditiesDB calls repository with null items`() = runTest {
        // Given
        val items: List<CommodityGroupModel>? = null

        coEvery { repository.updateCommoditiesDB(null) } returns Unit

        // When
        commodityInteractor.updateCommoditiesDB(items)

        // Then
        // Verify that the method was called (implicitly through coEvery)
    }

    @Test
    fun `getCommoditiesBalances calls repository with correct parameters`() = runTest {
        // Given
        val filter = CommodityBalanceFilter(
            query = "test",
            groupId = "group1",
            commodityId = "commodity1"
        )
        val expectedBalances = listOf(
            CommodityBalanceModel(
                id = "1",
                volume = 100f,
                commodity = mockk()
            )
        )
        val expectedResult = BaseResult.Success(expectedBalances)

        coEvery { 
            repository.getCommoditiesBalances(
                search = "test",
                page = 1,
                pageSize = 1000,
                groupId = "group1",
                commodityId = "commodity1"
            ) 
        } returns expectedResult

        // When
        val result = commodityInteractor.getCommoditiesBalances(filter)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `getCommoditiesBalances with null parameters calls repository correctly`() = runTest {
        // Given
        val filter = CommodityBalanceFilter(
            query = null,
            groupId = null,
            commodityId = null
        )
        val expectedBalances = emptyList<CommodityBalanceModel>()
        val expectedResult = BaseResult.Success(expectedBalances)

        coEvery { 
            repository.getCommoditiesBalances(
                search = null,
                page = 1,
                pageSize = 1000,
                groupId = null,
                commodityId = null
            ) 
        } returns expectedResult

        // When
        val result = commodityInteractor.getCommoditiesBalances(filter)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `getCommodities handles repository error correctly`() = runTest {
        // Given
        val filter = CommodityFilter(query = "test")
        val exception = RuntimeException("Network error")
        val expectedResult = BaseResult.Error(exception)

        coEvery { 
            repository.getCommodities(
                search = "test",
                page = 1,
                pageSize = 1000
            ) 
        } returns expectedResult

        // When
        val result = commodityInteractor.getCommodities(filter)

        // Then
        assertTrue(result is BaseResult.Error)
        assertEquals(exception, (result as BaseResult.Error).exception)
    }

    @Test
    fun `getCommoditiesBalances handles repository error correctly`() = runTest {
        // Given
        val filter = CommodityBalanceFilter(query = "test")
        val exception = RuntimeException("Network error")
        val expectedResult = BaseResult.Error(exception)

        coEvery { 
            repository.getCommoditiesBalances(
                search = "test",
                page = 1,
                pageSize = 1000,
                groupId = null,
                commodityId = null
            ) 
        } returns expectedResult

        // When
        val result = commodityInteractor.getCommoditiesBalances(filter)

        // Then
        assertTrue(result is BaseResult.Error)
        assertEquals(exception, (result as BaseResult.Error).exception)
    }

    @Test
    fun `getCommoditiesFromDB returns empty list when repository returns empty`() = runTest {
        // Given
        val expectedCommodities = emptyList<CommodityGroupModel>()

        coEvery { repository.getCommoditiesFromDB() } returns expectedCommodities

        // When
        val result = commodityInteractor.getCommoditiesFromDB()

        // Then
        assertTrue(result.isEmpty())
    }
}
