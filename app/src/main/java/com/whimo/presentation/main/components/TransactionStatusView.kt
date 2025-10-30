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
package com.whimo.presentation.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.whimo.R
import com.whimo.domain.transactions.models.TransactionStatus
import com.whimo.presentation.ui.theme.ColorBerryBlue
import com.whimo.presentation.ui.theme.ColorError
import com.whimo.presentation.ui.theme.ColorGray10
import com.whimo.presentation.ui.theme.ColorGray5
import com.whimo.presentation.ui.theme.ColorGray50
import com.whimo.presentation.ui.theme.ColorGray90
import com.whimo.presentation.ui.theme.ColorLightBlue
import com.whimo.presentation.ui.theme.ColorLightGreen
import com.whimo.presentation.ui.theme.ColorLightOrange
import com.whimo.presentation.ui.theme.ColorLightRed
import com.whimo.presentation.ui.theme.ColorSuccess
import com.whimo.presentation.ui.theme.ColorWarning
import com.whimo.presentation.ui.theme.TextStyleMediumS
import com.whimo.presentation.ui.theme.WhimoTheme

@Preview
@Composable
private fun Preview() {
    WhimoTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            TransactionStatusView(status = TransactionStatus.Accepted)
            TransactionStatusView(status = TransactionStatus.Rejected)
            TransactionStatusView(status = TransactionStatus.Pending)
            TransactionStatusView(status = TransactionStatus.NoResponse)
            TransactionStatusView(status = TransactionStatus.Recorded)
        }
    }
}

@Composable
fun TransactionStatusView(
    status: TransactionStatus,
    modifier: Modifier = Modifier
) {
    var bgColor = ColorLightGreen
    var text = stringResource(R.string.accepted)
    var textColor = ColorSuccess

    when (status) {
        TransactionStatus.Accepted -> {
            bgColor = ColorLightGreen
            text = stringResource(R.string.accepted)
            textColor = ColorSuccess
        }
        TransactionStatus.Rejected -> {
            bgColor = ColorLightRed
            text = stringResource(R.string.rejected)
            textColor = ColorError
        }
        TransactionStatus.Pending -> {
            bgColor = ColorLightOrange
            text = stringResource(R.string.pending)
            textColor = ColorWarning
        }
        TransactionStatus.NoResponse -> {
            bgColor = ColorGray5
            text = stringResource(R.string.no_response)
            textColor = ColorGray50
        }
        TransactionStatus.Recorded -> {
            bgColor = ColorLightBlue
            text = stringResource(R.string.recorded)
            textColor = ColorBerryBlue
        }
        TransactionStatus.Automatic -> {
            bgColor = ColorGray10
            text = stringResource(R.string.automatic)
            textColor = ColorGray90
        }
    }
    Box(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = TextStyleMediumS,
        )
    }
}