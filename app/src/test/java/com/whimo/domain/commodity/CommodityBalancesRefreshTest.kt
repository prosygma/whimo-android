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
import com.whimo.domain.commodity.models.CommodityBalanceFilter
import com.whimo.domain.commodity.models.CommodityBalanceModel
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommodityBalancesRefreshTest {
    private val interactor = mockk<CommodityInteractor>()

    @Test
    fun `publishes cache before remote request and preserves request filter snapshot`() = runTest {
        val filter = CommodityBalanceFilter(commodityId = "input")
        val original = filter.copy()
        val cached = listOf(mockk<CommodityBalanceModel>())
        val remote = BaseResult.Success(listOf(mockk<CommodityBalanceModel>()))
        var cachedPublished = false
        coEvery { interactor.getBalancesFromDB(original) } returns cached
        coEvery { interactor.getBalances(original) } answers {
            assertTrue(cachedPublished)
            remote
        }

        val result = interactor.refreshBalances(filter) {
            assertEquals(cached, it)
            cachedPublished = true
            filter.commodityId = "changed"
        }

        assertEquals(remote, result)
        coVerifyOrder {
            interactor.getBalancesFromDB(original)
            interactor.getBalances(original)
        }
    }

    @Test
    fun `remote error is returned without replacing published cache`() = runTest {
        val cached = listOf(mockk<CommodityBalanceModel>())
        val error = BaseResult.Error(IllegalStateException("offline"))
        coEvery { interactor.getBalancesFromDB(any()) } returns cached
        coEvery { interactor.getBalances(any()) } returns error
        val snapshots = mutableListOf<List<CommodityBalanceModel>>()

        val result = interactor.refreshBalances(CommodityBalanceFilter()) { snapshots += it }

        assertEquals(error, result)
        assertEquals(listOf(cached), snapshots)
    }

    @Test
    fun `cancelled request cannot return repository cache fallback to caller`() = runTest {
        coEvery { interactor.getBalancesFromDB(any()) } returns emptyList()
        coEvery { interactor.getBalances(any()) } coAnswers {
            try {
                awaitCancellation()
            } catch (_: CancellationException) {
                BaseResult.Success(emptyList())
            }
        }
        var completed = false
        val job = launch {
            interactor.refreshBalances(CommodityBalanceFilter()) {}
            completed = true
        }
        runCurrent()
        job.cancelAndJoin()
        assertFalse(completed)
    }
}
