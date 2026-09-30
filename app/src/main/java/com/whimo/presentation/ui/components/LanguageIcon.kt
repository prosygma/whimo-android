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
package com.whimo.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whimo.domain.languages.models.AppLanguage
import com.whimo.presentation.ui.models.iconRes
import com.whimo.presentation.ui.theme.TextStyleMediumXS

/**
 * Flag of a language: the bundled drawable, else the emoji set in the admin panel,
 * else a circle with the language code.
 */
@Composable
fun LanguageIcon(
    modifier: Modifier = Modifier,
    language: AppLanguage,
) {
    val iconRes = language.iconRes

    when {
        iconRes != null -> {
            Icon(
                modifier = modifier.size(24.dp),
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
            )
        }

        language.flag.isNotBlank() -> {
            Box(
                modifier = modifier.size(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = language.flag,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        else -> {
            Box(
                modifier = modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = language.code.substringBefore('-').uppercase().take(2),
                    style = TextStyleMediumXS.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
