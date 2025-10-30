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
package com.whimo.di

import com.whimo.data.AppDatabase
import com.whimo.data.commodity.repository.CommodityRepository
import com.whimo.data.commodity.repository.CommodityRepositoryImpl
import com.whimo.data.commodity.service.CommodityService
import com.whimo.domain.commodity.CommodityInteractor
import com.whimo.domain.commodity.CommodityInteractorImpl
import com.whimo.presentation.balances.CommodityGroupsViewModel
import com.whimo.presentation.balances.CommodityGroupBalancesViewModel
import com.whimo.presentation.createtransaction.commodity.CommodityTypesViewModel
import com.whimo.presentation.createtransaction.commodity.CommodityVolumeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

val commodityModule = module {
    // Services
    single { get<Retrofit>(AUTHORISED).create(CommodityService::class.java) }

    // DB
    single { get<AppDatabase>().commodityGroupsDao() }

    // Repositories
    factory<CommodityRepository> { CommodityRepositoryImpl(service = get(), dao = get()) }

    // Interactors
    factory<CommodityInteractor> { CommodityInteractorImpl(repository = get()) }

    // ViewModels
    viewModel { CommodityTypesViewModel(interactor = get(), errorHandler = get()) }
    viewModel { CommodityVolumeViewModel(interactor = get(), errorHandler = get(), resourceProvider = get()) }
    viewModel { CommodityGroupsViewModel(interactor = get(), errorHandler = get()) }
    viewModel { CommodityGroupBalancesViewModel(interactor = get(), errorHandler = get()) }
}