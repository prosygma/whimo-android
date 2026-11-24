package com.whimo.presentation.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.whimo.R
import com.whimo.domain.transactions.models.HarvestSeasonStatus
import com.whimo.presentation.ui.theme.TextStyleBodyS
import com.whimo.presentation.ui.theme.TextStyleMediumM
import com.whimo.presentation.ui.theme.WhimoTheme

data class ConversionItem(
    val title: String,
    val volume: String,
)

@Preview
@Composable
private fun Preview() {
    WhimoTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ConversionItemView(
                fromItems = listOf(
                    ConversionItem("Green bean", "-500kg"),
                ),
                toItems = listOf(
                    ConversionItem("Cocoa shells, husks, skins and other cocoa waste", "+150kg"),
                ),
                date = "Apr 22, 2026 11:10am",
                harvestSeasonText = "2025/26",
                harvestSeasonStatus = HarvestSeasonStatus.Active,
            )
            ConversionItemView(
                fromItems = listOf(
                    ConversionItem("Green bean", "-500kg"),
                    ConversionItem("Green bean", "-500kg"),
                ),
                toItems = listOf(
                    ConversionItem("Cocoa beans, whole or broken, raw or roasted", "+850kg"),
                    ConversionItem("Cocoa shells, husks, skins and other cocoa waste", "+150kg"),
                ),
                date = "Apr 22, 2026 11:10am",
                harvestSeasonText = "2025/26",
                harvestSeasonStatus = HarvestSeasonStatus.Active,
            )
        }
    }
}

@Composable
fun ConversionItemView(
    modifier: Modifier = Modifier,
    fromItems: List<ConversionItem>,
    toItems: List<ConversionItem>,
    date: String,
    harvestSeasonText: String? = null,
    harvestSeasonStatus: HarvestSeasonStatus? = null,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            modifier = Modifier.size(28.dp),
            painter = painterResource(id = R.drawable.ic_convert_28),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                fromItems.forEach {
                    FromItemView(it)
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    toItems.forEach {
                        ToItemView(it)
                    }
                }

                Text(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp),
                    text = "to",
                    style = TextStyleBodyS,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!harvestSeasonText.isNullOrBlank()) {
                    HarvestSeasonTag(
                        text = harvestSeasonText,
                        status = harvestSeasonStatus,
                    )
                }

                Text(
                    modifier = Modifier.weight(1f),
                    text = date,
                    style = TextStyleBodyS,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FromItemView(
    item: ConversionItem,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(36.dp),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = item.title,
            style = TextStyleMediumM,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = item.volume,
            style = TextStyleMediumM,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ToItemView(
    item: ConversionItem,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(36.dp),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = item.title,
            style = TextStyleBodyS,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = item.volume,
            style = TextStyleBodyS,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
