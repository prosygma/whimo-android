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
package com.whimo.presentation.routing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.whimo.R
import com.whimo.base.ObserveEffects
import com.whimo.presentation.auth.AuthActivity
import com.whimo.presentation.main.MainActivity
import com.whimo.presentation.ui.theme.BrandPalette
import com.whimo.presentation.ui.theme.TextStyleSplash1
import com.whimo.presentation.ui.theme.TextStyleSplash2
import com.whimo.presentation.ui.theme.WhimoTheme
import org.koin.androidx.viewmodel.ext.android.getViewModel

class RoutingActivity : ComponentActivity() {
    private lateinit var viewModel: RoutingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        splash.setKeepOnScreenCondition { false }

        setContent {
            viewModel = getViewModel()

            if (viewModel != null) {
                ObserveEffects(viewModel) { effect ->
                    when (effect) {
                        RoutingContract.Effect.NavigateAuth -> {
                            AuthActivity.openAuth(this@RoutingActivity)
                        }
                        RoutingContract.Effect.NavigateMain -> {
                            MainActivity.openMain(this@RoutingActivity)
                        }
                    }
                }
            }

            LifecycleEventEffect(event = Lifecycle.Event.ON_CREATE) {
                viewModel.setEvent(RoutingContract.Event.OnCreate)
            }

            SplashScreen()
        }
    }
}

@Preview
@Composable
private fun Preview() {
    WhimoTheme {
        SplashScreen()
    }
}

@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Brand flavors can show their logo above the name (integer/brand_splash_show_logo).
            if (integerResource(R.integer.brand_splash_show_logo) == 1) {
                Image(
                    painter = painterResource(R.drawable.brand_splash_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .size(180.dp),
                )
            }
            // A long product name shrinks to fit one line instead of overflowing.
            BasicText(
                text = stringResource(R.string.app_name),
                style = TextStyleSplash1.copy(color = BrandPalette.SurfaceDark, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = TextStyleSplash1.fontSize),
            )
            Text(
                text = stringResource(R.string.what_is_my_origin),
                style = TextStyleSplash2,
                color = BrandPalette.SurfaceDark,
                textAlign = TextAlign.Center,
            )
        }
    }
}
