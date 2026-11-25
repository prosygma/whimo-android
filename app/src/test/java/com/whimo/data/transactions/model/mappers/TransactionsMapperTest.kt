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

import com.whimo.data.commodity.model.response.Commodity
import com.whimo.data.harvestseasons.model.response.HarvestSeasonData
import com.whimo.data.transactions.model.response.ConversionTransactionData
import com.whimo.data.transactions.model.response.TransactionData
import com.whimo.extensions.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate

class TransactionsMapperTest {

    @Test
    fun `toDomain maps updated_at to updatedDate`() {
        val updatedAt = "2025-02-03T12:30:45Z"

        val result = transactionData(updatedAt = updatedAt).toDomain()

        assertEquals(updatedAt.toLocalDateTime(), result.updatedDate)
    }

    @Test
    fun `toDomain falls back to createdDate when updated_at is missing`() {
        val createdAt = "2025-02-03T10:00:00Z"

        val result = transactionData(createdAt = createdAt, updatedAt = null).toDomain()

        assertEquals(result.createdDate, result.updatedDate)
    }

    @Test
    fun `toDomain maps harvest season when response contains season object`() {
        val result = transactionData(
            harvestSeason = HarvestSeasonData(
                id = "season-id",
                name = "Cocoa 2025/26",
                start_date = "2025-09-01",
                end_date = "2026-09-01",
                status = "active",
            )
        ).toDomain()

        assertNotNull(result.harvestSeason)
        assertEquals("season-id", result.harvestSeason?.id)
        assertEquals("Cocoa 2025/26", result.harvestSeason?.name)
        assertEquals(LocalDate.of(2025, 9, 1), result.harvestSeason?.startDate)
        assertEquals(LocalDate.of(2026, 9, 1), result.harvestSeason?.endDate)
    }

    @Test
    fun `toDomain prefers transaction coordinates over legacy and farm coordinates`() {
        val result = transactionData(
            latitude = 1.0,
            longitude = 2.0,
            transactionLatitude = 3.0,
            transactionLongitude = 4.0,
            farmLatitude = 5.0,
            farmLongitude = 6.0,
        ).toDomain()

        assertEquals(3.0, result.location?.latitude())
        assertEquals(4.0, result.location?.longitude())
    }

    @Test
    fun `toDomain maps grouped conversion inputs and outputs`() {
        val result = transactionData(
            id = null,
            groupId = "conversion-group-id",
            type = "conversion",
            action = null,
            commodity = null,
            volume = null,
            signedVolume = null,
            inputs = listOf(
                conversionTransactionData(
                    commodityId = "input-id",
                    commodityCode = "1234",
                    commodityName = "Green bean",
                    volume = 1000f,
                    signedVolume = -1000f,
                )
            ),
            outputs = listOf(
                conversionTransactionData(
                    commodityId = "output-id",
                    commodityCode = "1801",
                    commodityName = "Cocoa beans",
                    volume = 850f,
                    signedVolume = 850f,
                )
            ),
        ).toDomain()

        assertEquals("conversion-group-id", result.id)
        assertEquals("conversion-group-id", result.groupId)
        assertEquals("Green bean", result.commodity.name)
        assertEquals(1, result.conversionInputs.size)
        assertEquals(-1000f, result.conversionInputs.first().signedVolume)
        assertEquals(1, result.conversionOutputs.size)
        assertEquals("Cocoa beans", result.conversionOutputs.first().commodity.name)
    }

    private fun transactionData(
        id: String? = "transaction-id",
        groupId: String? = null,
        createdAt: String = "2025-02-03T10:00:00Z",
        updatedAt: String? = "2025-02-03T12:30:45Z",
        type: String? = "downstream",
        action: String? = "buying",
        latitude: Double? = null,
        longitude: Double? = null,
        transactionLatitude: Double? = null,
        transactionLongitude: Double? = null,
        farmLatitude: Double? = null,
        farmLongitude: Double? = null,
        commodity: Commodity? = commodity(),
        volume: Float? = 10f,
        signedVolume: Float? = null,
        harvestSeason: HarvestSeasonData? = null,
        inputs: List<ConversionTransactionData>? = null,
        outputs: List<ConversionTransactionData>? = null,
    ) = TransactionData(
        id = id,
        group_id = groupId,
        created_at = createdAt,
        updated_at = updatedAt,
        expires_at = null,
        type = type,
        status = "accepted",
        action = action,
        location = "gps",
        latitude = latitude,
        longitude = longitude,
        transaction_latitude = transactionLatitude,
        transaction_longitude = transactionLongitude,
        farm_latitude = farmLatitude,
        farm_longitude = farmLongitude,
        commodity = commodity,
        volume = volume,
        signed_volume = signedVolume,
        traceability = "full",
        seller = null,
        buyer = null,
        is_buying_from_farmer = false,
        is_automatic = false,
        harvest_season = harvestSeason,
        created_by_id = "creator-id",
        inputs = inputs,
        outputs = outputs,
    )

    private fun conversionTransactionData(
        commodityId: String,
        commodityCode: String,
        commodityName: String,
        volume: Float,
        signedVolume: Float,
    ) = ConversionTransactionData(
        id = null,
        transaction_id = null,
        commodity = commodity(
            id = commodityId,
            code = commodityCode,
            name = commodityName,
        ),
        volume = volume,
        signed_volume = signedVolume,
    )

    private fun commodity(
        id: String = "commodity-id",
        code: String = "COCOA",
        name: String = "Cocoa",
    ) = Commodity(
        id = id,
        code = code,
        name = name,
        unit = "kg",
        has_recipe = false,
        group = null,
        balance = null,
    )
}
