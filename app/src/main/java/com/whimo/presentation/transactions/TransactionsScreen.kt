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
package com.whimo.presentation.transactions

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.whimo.R
import com.whimo.base.ObserveEffects
import com.whimo.domain.createtransaction.models.PendingTransactionModel
import com.whimo.domain.createtransaction.models.PendingTransactionsSyncProgress
import com.whimo.domain.createtransaction.models.getCommodityName
import com.whimo.domain.createtransaction.models.getCommodityVolumeTextWithSign
import com.whimo.domain.transactions.models.TransactionAction
import com.whimo.domain.transactions.models.TransactionListDownloadSummaryModel
import com.whimo.domain.transactions.models.TransactionModel
import com.whimo.domain.transactions.models.TransactionType
import com.whimo.domain.transactions.models.TransactionsState
import com.whimo.domain.transactions.models.getCommodityName
import com.whimo.domain.transactions.models.getCommodityVolumeTextWithSign
import com.whimo.domain.transactions.models.getConversionInputs
import com.whimo.domain.transactions.models.getConversionOutputs
import com.whimo.domain.transactions.models.getShortName
import com.whimo.domain.transactions.models.getVolumeTextWithSign
import com.whimo.domain.transactions.models.isConversion
import com.whimo.extensions.millisToLocalDateTime
import com.whimo.extensions.toFormattedDateString2
import com.whimo.extensions.toShortFormattedDateString
import com.whimo.navigation.Screens
import com.whimo.presentation.createtransaction.geodata.FarmGeoDataActivity
import com.whimo.presentation.main.components.ConversionItem
import com.whimo.presentation.main.components.ConversionItemView
import com.whimo.presentation.main.components.EmptyState
import com.whimo.presentation.main.components.HarvestSeasonFilterBottomSheet
import com.whimo.presentation.main.components.HarvestSeasonFilterChip
import com.whimo.presentation.main.components.LoadingState
import com.whimo.presentation.main.components.SearchFilterBar
import com.whimo.presentation.main.components.TabBar
import com.whimo.presentation.main.components.TabItem
import com.whimo.presentation.main.components.Toolbar
import com.whimo.presentation.main.components.TransactionItem
import com.whimo.presentation.notifications.NotificationsActivity
import com.whimo.presentation.settings.components.DialogButtonsItem
import com.whimo.presentation.transactions.transactiondetails.TransactionDetailsActivity
import com.whimo.presentation.transactions.transactiondetails.components.BaseDialog
import com.whimo.presentation.ui.baseScreen.MainIconButton
import com.whimo.presentation.ui.components.rememberNetworkAvailable
import com.whimo.presentation.ui.theme.TextStyleBodyS
import com.whimo.presentation.ui.theme.TextStyleButtonM
import com.whimo.presentation.ui.theme.TextStyleH2
import com.whimo.presentation.ui.theme.TextStyleMediumM
import com.whimo.presentation.ui.theme.WhimoTheme
import com.whimo.utils.getResult
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar

@Preview
@Composable
private fun Preview() {
    WhimoTheme {
        TransactionsScreen(
            modifier = Modifier.fillMaxSize(),
            navController = rememberNavController(),
            haveUnreadNotifications = false,
            viewModel = null,
        )
    }
}

private data class DownloadConfirmationState(
    val option: TransactionsDownloadOption,
    val summary: TransactionListDownloadSummaryModel,
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SimpleDateFormat")
@Composable
fun TransactionsScreen(
    modifier: Modifier,
    navController: NavHostController,
    haveUnreadNotifications: Boolean,
    viewModel: TransactionsViewModel? = koinViewModel(),
) {

    val tabs = TransactionsTab.entries.map {
        TabItem(title = stringResource(it.titleResId), icon = it.icon)
    }

    val binding = viewModel?.observeViewBinding() ?: TransactionsContract.Binding()
    val context = LocalContext.current
    val networkAvailable = rememberNetworkAvailable()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel?.setEvent(TransactionsContract.Event.Refresh(binding.currentTab))
            }
        }
    )

    val createZipFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel?.setEvent(TransactionsContract.Event.SaveDownloadedFile(context, uri))
        } else {
            viewModel?.setEvent(TransactionsContract.Event.SaveDownloadedFileCancelled)
        }
    }

    val createCSVFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel?.setEvent(TransactionsContract.Event.SaveDownloadedFile(context, uri))
        } else {
            viewModel?.setEvent(TransactionsContract.Event.SaveDownloadedFileCancelled)
        }
    }

    var showDownloadOptions by remember { mutableStateOf(false) }
    var downloadLoading by remember { mutableStateOf(false) }
    var downloadConfirmationState by remember { mutableStateOf<DownloadConfirmationState?>(null) }

    if (viewModel != null) {
        ObserveEffects(viewModel) { effect ->
            when (effect) {
                is TransactionsContract.Effect.NavigateTransactionDetails -> {
                    TransactionDetailsActivity.openTransactionDetails(context, launcher, effect.transaction)
                }
                is TransactionsContract.Effect.NavigateAddGeolocation -> {
                    FarmGeoDataActivity.openFarmGeoData(context, launcher, effect.transaction)
                }
                is TransactionsContract.Effect.NavigateNotifications -> {
                    NotificationsActivity.openNotifications(navController.context, launcher)
                }
                is TransactionsContract.Effect.ShowDownloadConfirmation -> {
                    downloadConfirmationState = DownloadConfirmationState(effect.option, effect.summary)
                }
                is TransactionsContract.Effect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }
                is TransactionsContract.Effect.ToggleDownloadLoader -> {
                    downloadLoading = effect.isLoading
                }
                is TransactionsContract.Effect.CSVDataDownloaded -> {
                    createCSVFileLauncher.launch(effect.filename)
                }
                is TransactionsContract.Effect.ZipDataDownloaded -> {
                    createZipFileLauncher.launch(effect.filename)
                }
            }
        }
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_CREATE) {
        viewModel?.setEvent(TransactionsContract.Event.OnCreate)
    }

    LaunchedEffect(networkAvailable) {
        viewModel?.setEvent(TransactionsContract.Event.NetworkAvailabilityChanged(networkAvailable))
    }

    if (navController.getResult<Boolean>("createTransactionResult") == true) {
        viewModel?.setEvent(TransactionsContract.Event.Refresh(binding.currentTab))
    }

    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val tab = TransactionsTab.entries[page]
            viewModel?.setEvent(TransactionsContract.Event.TabChanged(tab))
        }
    }

    var showCalendar by remember { mutableStateOf(false) }
    var showSeasonFilter by remember { mutableStateOf(false) }
    val datePickerState = rememberDateRangePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= Calendar.getInstance().timeInMillis
            }
        }
    )

    Column(
        modifier = modifier,
    ) {
        Toolbar(
            title = stringResource(R.string.transactions_title),
            startIconRes = R.drawable.ic_download,
            iconRes = if (haveUnreadNotifications) R.drawable.ic_notification_dot else R.drawable.ic_notification,
            onStartIconClick = {
                showDownloadOptions = true
            },
        ) {
            viewModel?.setEvent(TransactionsContract.Event.NotificationsClicked)
        }

        val harvestSeasonFilterIsActive = binding.selectedCommodityGroup != null || binding.selectedHarvestSeason != null

        SearchFilterBar(
            modifier = Modifier.padding(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = if (harvestSeasonFilterIsActive) 8.dp else 16.dp,
            ),
            query = binding.query ?: "",

            showDateFilter = true,
            dateFilterIsActive = datePickerState.selectedStartDateMillis != null,
            filterIsActive = harvestSeasonFilterIsActive,
            onSearch = {
                viewModel?.setEvent(TransactionsContract.Event.QueryChanged(it))
            },
            onDateFilterClick = {
                showCalendar = true
            },
            onFilterClick = {
                showSeasonFilter = true
            }
        )

        if (harvestSeasonFilterIsActive) {
            HarvestSeasonFilterChip(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                commodityGroup = binding.selectedCommodityGroup,
                harvestSeason = binding.selectedHarvestSeason,
                onClear = {
                    viewModel?.setEvent(TransactionsContract.Event.HarvestSeasonFilterChanged(null, null))
                },
            )
        }

        TabBar(
            pagerState = pagerState,
            tabs = tabs,
        )

        Box {
            HorizontalPager(
                state = pagerState,
            ) { index ->
                val tab = TransactionsTab.entries[index]
                val tabState = binding.tabStates[tab]
                val pendingTransactions = binding.pendingTransactions[tab]

                TransactionPage(
                    tabState = tabState!!,
                    pendingTransactions = pendingTransactions!!,
                    onRefresh = {
                        viewModel?.setEvent(TransactionsContract.Event.Refresh(tab))
                    },
                    onLoadMore = {
                        viewModel?.setEvent(TransactionsContract.Event.NextPage(tab))
                    },
                    onTransactionClick = {
                        viewModel?.setEvent(TransactionsContract.Event.TransactionClicked(it))
                    },
                    onAddLocationClick = {
                        viewModel?.setEvent(TransactionsContract.Event.AddGeoDataClicked(it))
                    },
                    onAddTransactionClick = {
                        navController.navigate(Screens.CreateTransaction.route)
                    }
                )
            }

            val tab = TransactionsTab.entries[pagerState.currentPage]
            val tabState = binding.tabStates[tab]

            if (tabState is TransactionsState.Success || tabState is TransactionsState.End) {
                FloatingActionButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    shape = FloatingActionButtonDefaults.smallShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.loweredElevation(),
                    onClick = {
                        navController.navigate(Screens.CreateTransaction.route)
                    },
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.ic_add_transaction),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }

    if (showSeasonFilter) {
        HarvestSeasonFilterBottomSheet(
            commodityGroups = binding.commodityGroups,
            harvestSeasons = binding.harvestSeasons,
            selectedCommodityGroup = binding.selectedCommodityGroup,
            selectedSeason = binding.selectedHarvestSeason,
            onCommodityGroupPreviewChanged = {
                viewModel?.setEvent(TransactionsContract.Event.HarvestSeasonFilterCommodityGroupChanged(it))
            },
            onApply = { commodityGroup, harvestSeason ->
                viewModel?.setEvent(
                    TransactionsContract.Event.HarvestSeasonFilterChanged(
                        commodityGroup = commodityGroup,
                        harvestSeason = harvestSeason,
                    )
                )
            },
            onReset = {
                viewModel?.setEvent(TransactionsContract.Event.HarvestSeasonFilterChanged(null, null))
            },
            onDismissRequest = { showSeasonFilter = false },
        )
    }

    if (showCalendar) {
        val startDate = datePickerState.selectedStartDateMillis?.millisToLocalDateTime()
        val endDate = datePickerState.selectedEndDateMillis?.millisToLocalDateTime()?.plusDays(1)?.minusSeconds(1)

        var rangeText = ""

        if (startDate != null && endDate != null) {
            rangeText = startDate.toShortFormattedDateString() + " - " + endDate.toShortFormattedDateString()

        } else if (startDate != null) {
            rangeText = startDate.toShortFormattedDateString()
        }

        datePickerState.displayedMonthMillis = datePickerState.selectedStartDateMillis ?: Calendar.getInstance().timeInMillis

        BaseDialog(
            title = stringResource(R.string.select_dates),
            onDismiss = { showCalendar = false }
        ) {
            if (startDate != null) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    text = rangeText,
                    style = TextStyleMediumM,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            DateRangePicker(
                modifier = Modifier.height(500.dp),
                state = datePickerState,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors().copy(
                    dayInSelectionRangeContentColor = MaterialTheme.colorScheme.onSecondary,
                    dayInSelectionRangeContainerColor = MaterialTheme.colorScheme.secondary,
                )
            )

            DialogButtonsItem(
                actionButtonTitle = stringResource(R.string.confirm),
                actionButtonBackgroundColor = MaterialTheme.colorScheme.primary,
                actionButtonTitleColor = MaterialTheme.colorScheme.onPrimary,
                secondButtonTitle = stringResource(R.string.clear),
                onActionClick = {
                    showCalendar = false
                    viewModel?.setEvent(TransactionsContract.Event.DatesChanged(startDate, endDate))
                },
                onSecondClick = {
                    datePickerState.setSelection(null, null)
                    viewModel?.setEvent(TransactionsContract.Event.DatesChanged())
                    showCalendar = false
                },
            )
        }
    }

    binding.pendingSyncProgress?.let { progress ->
        PendingTransactionsSyncDialog(progress = progress)
    }

    if (showDownloadOptions) {
        DownloadAllOptionsDialog(
            onDismissRequest = { showDownloadOptions = false },
            onOptionSelected = { option ->
                showDownloadOptions = false
                viewModel?.setEvent(TransactionsContract.Event.DownloadOptionSelected(option))
            },
        )
    }

    downloadConfirmationState?.let { state ->
        DownloadConfirmationDialog(
            option = state.option,
            summary = state.summary,
            onDismiss = { downloadConfirmationState = null },
            onDownload = {
                downloadConfirmationState = null
                viewModel?.setEvent(TransactionsContract.Event.DownloadConfirmed(state.option))
            },
        )
    }

    if (downloadLoading) {
        LoadingState()
    }
}

@Composable
private fun PendingTransactionsSyncDialog(
    progress: PendingTransactionsSyncProgress,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.offline_transactions_sync_title),
                        style = TextStyleH2,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(
                            R.string.offline_transactions_sync_progress,
                            progress.processedTransactions,
                            progress.totalTransactions,
                        ),
                        style = TextStyleBodyS,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadAllOptionsDialog(
    onDismissRequest: () -> Unit,
    onOptionSelected: (TransactionsDownloadOption) -> Unit,
) {
    BaseDialog(
        title = stringResource(R.string.download_both),
        onDismiss = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.download_all_options_description),
                style = TextStyleBodyS,
                color = MaterialTheme.colorScheme.onSurface,
            )

            DownloadOptionCard(
                iconRes = R.drawable.ic_list,
                title = stringResource(R.string.download_transaction_details),
                onClick = { onOptionSelected(TransactionsDownloadOption.TransactionDetails) },
            )

            DownloadOptionCard(
                iconRes = R.drawable.ic_map_marker,
                title = stringResource(R.string.download_farm_geolocation),
                onClick = { onOptionSelected(TransactionsDownloadOption.FarmGeolocations) },
            )

            DownloadOptionCard(
                iconRes = R.drawable.ic_download,
                title = stringResource(R.string.download_both),
                onClick = { onOptionSelected(TransactionsDownloadOption.Both) },
            )
        }
    }
}

@Composable
private fun DownloadOptionCard(
    iconRes: Int,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = title,
            style = TextStyleButtonM,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DownloadConfirmationDialog(
    option: TransactionsDownloadOption,
    summary: TransactionListDownloadSummaryModel,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
) {
    BaseDialog(
        title = stringResource(option.confirmationTitleRes()),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(option.confirmationDescriptionRes(), summary.traders),
                style = TextStyleBodyS,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (option != TransactionsDownloadOption.TransactionDetails) {
                DownloadSummaryBullet(
                    text = stringResource(
                        R.string.download_all_merged_geojson_files,
                        summary.fullTraceabilityFarms,
                    )
                )
                DownloadSummaryBullet(
                    text = stringResource(
                        R.string.download_all_custom_location_files,
                        summary.manuallyAddedFarms,
                    )
                )

                if (summary.farmsWithoutGeolocation > 0) {
                    Text(
                        text = stringResource(
                            R.string.download_all_without_geolocation,
                            summary.farmsWithoutGeolocation,
                        ),
                        style = TextStyleBodyS,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        DialogButtonsItem(
            actionButtonTitle = stringResource(R.string.download),
            actionButtonBackgroundColor = MaterialTheme.colorScheme.primary,
            actionButtonTitleColor = MaterialTheme.colorScheme.onPrimary,
            onActionClick = onDownload,
            onSecondClick = onDismiss,
        )
    }
}

@Composable
private fun DownloadSummaryBullet(
    text: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "\u2022",
            style = TextStyleBodyS,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = TextStyleBodyS,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun TransactionsDownloadOption.confirmationTitleRes(): Int {
    return when (this) {
        TransactionsDownloadOption.TransactionDetails -> R.string.download_transaction_details_q
        TransactionsDownloadOption.FarmGeolocations -> R.string.download_farm_geolocation_q
        TransactionsDownloadOption.Both -> R.string.download_both_q
    }
}

private fun TransactionsDownloadOption.confirmationDescriptionRes(): Int {
    return when (this) {
        TransactionsDownloadOption.TransactionDetails -> R.string.downloading_transaction_details
        TransactionsDownloadOption.FarmGeolocations -> R.string.download_all_geolocations_confirmation
        TransactionsDownloadOption.Both -> R.string.download_all_both_confirmation
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionPage(
    tabState: TransactionsState,
    pendingTransactions: List<PendingTransactionModel>,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onTransactionClick: (TransactionModel) -> Unit = {},
    onAddLocationClick: (TransactionModel) -> Unit = {},
    onAddTransactionClick: () -> Unit = {},
) {
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        isRefreshing = tabState is TransactionsState.Reloading,
        onRefresh = onRefresh,
    ) {
        when (tabState) {
            TransactionsState.Empty -> {
                EmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    iconRes = R.drawable.ic_empty,
                    title = stringResource(R.string.no_transactions),
                    description = stringResource(R.string.no_transactions_description),
                ) {
                    MainIconButton(
                        modifier = Modifier.padding(top = 24.dp),
                        iconRes = R.drawable.ic_add_transaction,
                        title = stringResource(R.string.add_transaction),
                        onClick = onAddTransactionClick,
                    )
                }
            }
            TransactionsState.Reloading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = MaterialTheme.colorScheme.surface)
                )
            }
            is TransactionsState.PageLoading -> {
                TransactionsList(
                    listState = listState,
                    pendingTransactions = pendingTransactions,
                    transactions = tabState.transactions,
                    isLoading = true,
                    onTransactionClick = onTransactionClick,
                    onAddLocationClick = onAddLocationClick,
                )
            }
            is TransactionsState.End -> {
                TransactionsList(
                    listState = listState,
                    pendingTransactions = pendingTransactions,
                    transactions = tabState.transactions,
                    isEnd = true,
                    onTransactionClick = onTransactionClick,
                    onAddLocationClick = onAddLocationClick,
                )
            }
            is TransactionsState.Success -> {
                TransactionsList(
                    listState = listState,
                    pendingTransactions = pendingTransactions,
                    transactions = tabState.transactions,
                    onLoadMore = onLoadMore,
                    onTransactionClick = onTransactionClick,
                    onAddLocationClick = onAddLocationClick,
                )
            }
            is TransactionsState.Error -> {
                EmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    iconRes = R.drawable.ic_empty,
                    title = stringResource(R.string.error),
                    description = tabState.errorMessage
                )
            }
        }
    }
}

@Composable
fun TransactionsList(
    listState: LazyListState,
    pendingTransactions: List<PendingTransactionModel>,
    transactions: List<TransactionModel>,
    isLoading: Boolean = false,
    isEnd: Boolean = false,
    onLoadMore: () -> Unit = {},
    onPendingTransactionClick: (PendingTransactionModel) -> Unit = {},
    onTransactionClick: (TransactionModel) -> Unit = {},
    onAddLocationClick: (TransactionModel) -> Unit = {},
) {
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = listState.layoutInfo.totalItemsCount
            val nearEnd = lastVisibleIndex >= totalItems - 1
            nearEnd && !isLoading
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { shouldLoadMore }
            .distinctUntilChanged()
            .collect { load ->
                if (load) onLoadMore()
            }
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(pendingTransactions) {
            val transaction = it.transactionModel
            val icon = if (transaction.action == TransactionAction.Buying) {
                R.drawable.ic_buy_28
            } else {
                R.drawable.ic_sell_28
            }
            TransactionItem(
                iconRes = icon,
                title = transaction.getCommodityName() ?: "",
                volume = transaction.getCommodityVolumeTextWithSign() ?: "",
                description = stringResource(R.string.offline_transaction),
                onClick = { onPendingTransactionClick(it) },
            )
        }

        items(
            items = transactions,
            key = { it.id }
        ) {
            if (it.isConversion()) {
                ConversionItemView(
                    fromItems = it.getConversionInputs().map { item ->
                        ConversionItem(
                            title = item.getCommodityName(),
                            volume = item.getVolumeTextWithSign(defaultSign = -1),
                        )
                    },
                    toItems = it.getConversionOutputs().map { item ->
                        ConversionItem(
                            title = item.getCommodityName(),
                            volume = item.getVolumeTextWithSign(defaultSign = 1),
                        )
                    },
                    date = it.createdDate.toFormattedDateString2(),
                    harvestSeasonText = it.harvestSeason?.getShortName(),
                    harvestSeasonStatus = it.harvestSeason?.status,
                    onClick = { onTransactionClick(it) },
                )
            } else {
                val icon = if (it.action == TransactionAction.Buying) {
                    R.drawable.ic_buy_28
                } else {
                    R.drawable.ic_sell_28
                }
                TransactionItem(
                    iconRes = icon,
                    title = it.getCommodityName(),
                    volume = it.getCommodityVolumeTextWithSign(),
                    description = it.createdDate.toFormattedDateString2(),
                    harvestSeasonText = it.harvestSeason?.getShortName(),
                    harvestSeasonStatus = it.harvestSeason?.status,
                    status = it.status,
                    showAddLocation = it.type == TransactionType.Producer && it.locationProvider == null,
                    onClick = { onTransactionClick(it) },
                    onAddLocationClick = { onAddLocationClick(it) }
                )
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (isEnd) {
            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    text = stringResource(R.string.end),
                    style = TextStyleBodyS,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
