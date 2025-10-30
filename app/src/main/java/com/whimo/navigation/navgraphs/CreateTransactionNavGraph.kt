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
package com.whimo.navigation.navgraphs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.whimo.navigation.Screens
import com.whimo.presentation.createtransaction.CreateTransactionScreen2
import com.whimo.presentation.createtransaction.CreateTransactionScreen3
import com.whimo.presentation.createtransaction.CreateTransactionScreen4
import com.whimo.presentation.createtransaction.SharedTransactionViewModel
import com.whimo.presentation.createtransaction.commodity.CommodityTypesScreen
import com.whimo.presentation.createtransaction.commodity.CommodityVolumeScreen
import com.whimo.presentation.createtransaction.transactionform.CreateTransactionFormScreen
import com.whimo.presentation.createtransaction.userinfo.UserInfoScreen
import com.whimo.presentation.ui.components.AppNavHost

@Composable
fun CreateTransactionNavGraph(
    modifier: Modifier,
    navController: NavHostController,
    sharedViewModel: SharedTransactionViewModel,
    startDestination: String,
) {
    AppNavHost(
        modifier = modifier.fillMaxSize(),
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(route = Screens.CreateTransaction2.route) {
            CreateTransactionScreen2(modifier.fillMaxSize(), navController = navController, sharedTransactionViewModel = sharedViewModel)
        }
        composable(route = Screens.CreateTransaction3.route) {
            CreateTransactionScreen3(modifier.fillMaxSize(), navController = navController, sharedTransactionViewModel = sharedViewModel)
        }
        composable(route = Screens.CreateTransaction4.route) {
            CreateTransactionScreen4(modifier.fillMaxSize(), navController = navController, sharedTransactionViewModel = sharedViewModel)
        }
        composable(route = Screens.CreateTransactionForm.route) {
            CreateTransactionFormScreen(modifier.fillMaxSize(), navController = navController, sharedTransactionViewModel = sharedViewModel)
        }
        composable(route = Screens.CommodityTypes.route) {
            CommodityTypesScreen(modifier.fillMaxSize(), navController = navController, sharedTransactionViewModel = sharedViewModel)
        }
        composable(route = Screens.CommodityVolume.route) {
            CommodityVolumeScreen(modifier.fillMaxSize(), navController = navController, sharedTransactionViewModel = sharedViewModel)
        }
        composable(route = Screens.SupplierInfo.route) {
            UserInfoScreen(modifier.fillMaxSize(), navController = navController, sharedTransactionViewModel = sharedViewModel, isInvite = false)
        }
        composable(route = Screens.InviteUser.route) {
            UserInfoScreen(modifier.fillMaxSize(), navController = navController, sharedTransactionViewModel = sharedViewModel, isInvite = true)
        }
    }
}