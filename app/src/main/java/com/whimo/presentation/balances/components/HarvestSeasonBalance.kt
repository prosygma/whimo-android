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
package com.whimo.presentation.balances.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.whimo.R
import com.whimo.domain.harvestseasons.models.HarvestSeasonModel
import com.whimo.domain.harvestseasons.models.HarvestSeasonStatus
import com.whimo.domain.transactions.models.getShortName
import com.whimo.extensions.toQuantityTextWithUnit
import com.whimo.presentation.main.components.HarvestSeasonTag
import com.whimo.presentation.ui.theme.TextStyleBodyXS
import com.whimo.presentation.ui.theme.WhimoTheme

@Composable
fun HarvestSeasonBalance(
    season: HarvestSeasonModel,
    balance: Float?,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HarvestSeasonTag(text = season.getShortName(), status = season.status)
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.season_balance, balance?.toQuantityTextWithUnit(unit) ?: "—"),
            style = TextStyleBodyXS,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PreviewHarvestSeasonBalance() {
    WhimoTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val season = HarvestSeasonModel("season", "2025/26", HarvestSeasonStatus.Past)
            HarvestSeasonBalance(season = season, balance = 1000f, unit = "kg")
            HarvestSeasonBalance(season = season, balance = 0f, unit = "kg")
            HarvestSeasonBalance(season = season, balance = null, unit = "kg")
        }
    }
}
