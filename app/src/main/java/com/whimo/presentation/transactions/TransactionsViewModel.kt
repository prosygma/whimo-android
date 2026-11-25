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

import android.content.Context
import android.net.Uri
import com.whimo.R
import com.whimo.base.BaseViewModel
import com.whimo.base.CoreViewEvent
import com.whimo.data.base.common.onError
import com.whimo.data.base.common.onSuccess
import com.whimo.domain.commodity.CommodityInteractor
import com.whimo.domain.commodity.models.CommodityFilter
import com.whimo.domain.commodity.models.CommodityGroupModel
import com.whimo.domain.createtransaction.CreateTransactionInteractor
import com.whimo.domain.createtransaction.models.PendingTransactionModel
import com.whimo.domain.createtransaction.models.PendingTransactionsSyncProgress
import com.whimo.domain.harvestseasons.HarvestSeasonsInteractor
import com.whimo.domain.harvestseasons.models.HarvestSeasonFilter
import com.whimo.domain.transactions.TransactionsInteractor
import com.whimo.domain.transactions.models.HarvestSeasonModel
import com.whimo.domain.transactions.models.TransactionAction
import com.whimo.domain.transactions.models.TransactionListDownloadSummaryModel
import com.whimo.domain.transactions.models.TransactionModel
import com.whimo.domain.transactions.models.TransactionStatus
import com.whimo.domain.transactions.models.TransactionType
import com.whimo.domain.transactions.models.TransactionsFilter
import com.whimo.domain.transactions.models.TransactionsState
import com.whimo.network.ErrorHandler
import com.whimo.providers.ResourceProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import okhttp3.ResponseBody
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime

class TransactionsViewModel(
    private val allTabInteractor: TransactionsInteractor,
    private val boughtTabInteractor: TransactionsInteractor,
    private val soldTabInteractor: TransactionsInteractor,
    private val convertedTabInteractor: TransactionsInteractor,
    private val createTransactionInteractor: CreateTransactionInteractor,
    private val harvestSeasonsInteractor: HarvestSeasonsInteractor,
    private val commodityInteractor: CommodityInteractor,
    private val errorHandler: ErrorHandler,
    private val resourceProvider: ResourceProvider,
) : BaseViewModel<TransactionsContract.Binding>() {

    private var searchJob: Job? = null
    private var pendingSyncJob: Job? = null

    private var currentTab = TransactionsTab.entries.first()
    private var tabStates: MutableMap<TransactionsTab, TransactionsState>
    private var pendingTransactions: MutableMap<TransactionsTab, List<PendingTransactionModel>>
    private var networkAvailable: Boolean? = null
    private var commodityGroups: List<CommodityGroupModel> = emptyList()
    private var harvestSeasons: List<HarvestSeasonModel> = emptyList()
    private var harvestSeasonCommodityIds: List<String>? = null
    private val filter = TransactionsFilter()
    private var downloadedCSV: ResponseBody? = null
    private var downloadedBundle: ResponseBody? = null
    private var downloadedFileName = ""
    private var downloadBundleAfterCsvSave = false

    override fun createBinding(): TransactionsContract.Binding {
        return TransactionsContract.Binding()
    }

    override fun handleEvents(event: CoreViewEvent) {
        super.handleEvents(event)
        when (event) {
            is TransactionsContract.Event.OnCreate -> onCreate()
            is TransactionsContract.Event.TabChanged -> tabChanged(event.tab)
            is TransactionsContract.Event.Refresh -> refresh(event.tab)
            is TransactionsContract.Event.NextPage -> nextPage(event.tab)
            is TransactionsContract.Event.QueryChanged -> queryChanged(event.query)
            is TransactionsContract.Event.HarvestSeasonFilterCommodityGroupChanged -> {
                loadHarvestSeasons(event.commodityGroup)
            }
            is TransactionsContract.Event.HarvestSeasonFilterChanged -> {
                harvestSeasonFilterChanged(event.commodityGroup, event.harvestSeason)
            }
            is TransactionsContract.Event.HarvestSeasonChanged -> harvestSeasonChanged(event.harvestSeason)
            is TransactionsContract.Event.DatesChanged -> datesChanged(event.dateStart, event.dateEnd)
            is TransactionsContract.Event.StatusChanged -> statusChanged(event.status)
            is TransactionsContract.Event.TransactionClicked -> transactionClicked(event.transaction)
            is TransactionsContract.Event.AddGeoDataClicked -> addGeoDataClicked(event.transaction)
            is TransactionsContract.Event.NotificationsClicked -> notificationsClicked()
            is TransactionsContract.Event.DownloadOptionSelected -> downloadOptionSelected(event.option)
            is TransactionsContract.Event.DownloadConfirmed -> downloadConfirmed(event.option)
            is TransactionsContract.Event.SaveDownloadedFile -> saveDownloadedFile(event.context, event.uri)
            is TransactionsContract.Event.SaveDownloadedFileCancelled -> clearDownloadedFiles()
            is TransactionsContract.Event.NetworkAvailabilityChanged -> {
                networkAvailabilityChanged(event.isAvailable)
            }
        }
    }

    override fun copyBinding(binding: TransactionsContract.Binding): TransactionsContract.Binding {
        return binding.copy()
    }

    init {
        tabStates = TransactionsTab.entries
            .associateWith { TransactionsState.Empty }
            .toMutableMap()

        pendingTransactions = TransactionsTab.entries
            .associateWith { emptyList<PendingTransactionModel>() }
            .toMutableMap()

        launch {
            allTabInteractor.stateFlow.collect { state ->
                tabStates[TransactionsTab.All] = state

                delay(100)
                updateHarvestSeasonsFromTransactionsIfNeeded()
                updateTabs()
            }
        }
        launch {
            boughtTabInteractor.stateFlow.collect { state ->
                tabStates[TransactionsTab.Bought] = state

                delay(100)
                updateHarvestSeasonsFromTransactionsIfNeeded()
                updateTabs()
            }
        }
        launch {
            soldTabInteractor.stateFlow.collect { state ->
                tabStates[TransactionsTab.Sold] = state

                delay(100)
                updateHarvestSeasonsFromTransactionsIfNeeded()
                updateTabs()
            }
        }
        launch {
            convertedTabInteractor.stateFlow.collect { state ->
                tabStates[TransactionsTab.Converted] = state

                delay(100)
                updateHarvestSeasonsFromTransactionsIfNeeded()
                updateTabs()
            }
        }
    }

    private fun updateTabs() {
        updateBinding { b ->
            b.tabStates = tabStates
            b.pendingTransactions = pendingTransactions
            b.commodityGroups = commodityGroups
            b.selectedCommodityGroup = filter.commodityGroup
            b.harvestSeasons = harvestSeasons
            b.selectedHarvestSeason = filter.harvestSeason
        }
    }

    private fun updateHarvestSeasonsFromTransactionsIfNeeded() {
        if (harvestSeasons.isNotEmpty()) return
        if (harvestSeasonCommodityIds != filter.commodityGroup.commodityIdsOrNull()) return

        harvestSeasons = collectHarvestSeasons(filter.commodityGroup)
    }

    private fun onCreate() {
        updateBinding { b ->
            b.currentTab = currentTab
            b.tabStates = tabStates
            b.pendingTransactions = pendingTransactions

            b.query = filter.query
            b.commodityGroups = commodityGroups
            b.selectedCommodityGroup = filter.commodityGroup
            b.harvestSeasons = harvestSeasons
            b.selectedHarvestSeason = filter.harvestSeason

            b.dateStart = filter.dateStart
            b.dateEnd = filter.dateEnd
            b.status = filter.status
        }

        loadCommodityGroups()
        loadHarvestSeasons(filter.commodityGroup)
    }

    private fun loadCommodityGroups() {
        launch {
            val cachedCommodityGroups = commodityInteractor.getCommoditiesFromDB()
            if (cachedCommodityGroups.isNotEmpty()) {
                commodityGroups = cachedCommodityGroups
                filter.commodityGroup = filter.commodityGroup.refreshFrom(commodityGroups)
                updateTabs()
            }

            commodityInteractor.getCommodities(CommodityFilter())
                .onSuccess { groups ->
                    commodityGroups = groups.orEmpty()
                    filter.commodityGroup = filter.commodityGroup.refreshFrom(commodityGroups)
                    commodityInteractor.updateCommoditiesDB(groups)
                    updateTabs()
                }
        }
    }

    private fun loadHarvestSeasons(commodityGroup: CommodityGroupModel?) {
        val commodityIds = commodityGroup.commodityIdsOrNull()

        if (commodityGroup != null && commodityIds.isNullOrEmpty()) {
            harvestSeasonCommodityIds = commodityIds
            harvestSeasons = emptyList()
            updateTabs()
            return
        }

        launch {
            harvestSeasonCommodityIds = commodityIds
            val seasonFilter = HarvestSeasonFilter(commodityIds = commodityIds)
            val cachedSeasons = harvestSeasonsInteractor.getHarvestSeasonsFromDB(seasonFilter)
                .ifEmpty { collectHarvestSeasons(commodityGroup) }

            harvestSeasons = cachedSeasons
            updateTabs()

            harvestSeasonsInteractor.getHarvestSeasons(seasonFilter)
                .onSuccess { seasons ->
                    if (harvestSeasonCommodityIds != commodityIds) return@onSuccess

                    val loadedSeasons = seasons.orEmpty()
                    val selectedSeasonId = filter.harvestSeason?.id
                    if (filter.commodityGroup.sameGroupAs(commodityGroup)
                        && selectedSeasonId != null
                        && loadedSeasons.none { it.id == selectedSeasonId }
                    ) {
                        filter.harvestSeason = null
                    }
                    harvestSeasons = loadedSeasons
                    updateTabs()
                }
                .onError {
                    if (harvestSeasonCommodityIds != commodityIds) return@onError

                    harvestSeasons = cachedSeasons
                    updateTabs()
                }
        }
    }

    private fun tabChanged(tab: TransactionsTab) {
        currentTab = tab

        if (tabStates[tab] == TransactionsState.Empty) {
            refresh(tab)
        }
    }

    private fun refresh(tab: TransactionsTab) {
        refreshTransactions(tab)
        showPendingTransactions()
        syncPendingTransactions(refreshAfterSync = true)
    }

    private fun refreshTransactions(tab: TransactionsTab) {
        launch {
            when (tab) {
                TransactionsTab.All -> allTabInteractor.refresh(filter.forTab(tab))
                TransactionsTab.Bought -> boughtTabInteractor.refresh(filter.forTab(tab))
                TransactionsTab.Sold -> soldTabInteractor.refresh(filter.forTab(tab))
                TransactionsTab.Converted -> convertedTabInteractor.refresh(filter.forTab(tab))
            }
        }
    }

    private fun nextPage(tab: TransactionsTab) {
        launch {
            when (tab) {
                TransactionsTab.All -> allTabInteractor.loadNextPage(filter.forTab(tab))
                TransactionsTab.Bought -> boughtTabInteractor.loadNextPage(filter.forTab(tab))
                TransactionsTab.Sold -> soldTabInteractor.loadNextPage(filter.forTab(tab))
                TransactionsTab.Converted -> convertedTabInteractor.loadNextPage(filter.forTab(tab))
            }
        }
    }

    private fun queryChanged(query: String) {
        filter.query = query
        updateBinding { b ->
            b.query = query
        }

        searchJob?.cancel()
        searchJob = launch {
            delay(600)
            refresh(currentTab)
        }
    }

    private fun harvestSeasonChanged(harvestSeason: HarvestSeasonModel?) {
        harvestSeasonFilterChanged(filter.commodityGroup, harvestSeason)
    }

    private fun harvestSeasonFilterChanged(
        commodityGroup: CommodityGroupModel?,
        harvestSeason: HarvestSeasonModel?,
    ) {
        filter.commodityGroup = commodityGroup
        filter.commodity = null
        filter.harvestSeason = harvestSeason
        updateBinding { b ->
            b.selectedCommodityGroup = commodityGroup
            b.selectedHarvestSeason = harvestSeason
        }
        loadHarvestSeasons(commodityGroup)
        refresh(currentTab)
    }

    private fun datesChanged(dateStart: LocalDateTime?, dateEnd: LocalDateTime?) {
        filter.dateStart = dateStart
        filter.dateEnd = dateEnd
        refresh(currentTab)
    }

    private fun statusChanged(status: TransactionStatus) {
        filter.status = status
        refresh(currentTab)
    }

    private fun transactionClicked(transaction: TransactionModel) {
        setEffect(TransactionsContract.Effect.NavigateTransactionDetails(transaction))
    }

    private fun addGeoDataClicked(transaction: TransactionModel) {
        setEffect(TransactionsContract.Effect.NavigateAddGeolocation(transaction))
    }

    private fun notificationsClicked() {
        setEffect(TransactionsContract.Effect.NavigateNotifications)
    }

    private fun downloadOptionSelected(option: TransactionsDownloadOption) {
        setEffect(TransactionsContract.Effect.ToggleDownloadLoader(true))

        launch {
            getInteractor(currentTab).getDownloadSummary(filter.forTab(currentTab))
                .onSuccess { summary ->
                    setEffect(
                        TransactionsContract.Effect.ToggleDownloadLoader(false),
                        TransactionsContract.Effect.ShowDownloadConfirmation(
                            option = option,
                            summary = summary ?: TransactionListDownloadSummaryModel(),
                        )
                    )
                }
                .onError {
                    setEffect(
                        TransactionsContract.Effect.ToggleDownloadLoader(false),
                        TransactionsContract.Effect.ShowMessage(errorHandler.parseError(it))
                    )
                }
        }
    }

    private fun downloadConfirmed(option: TransactionsDownloadOption) {
        clearDownloadedFiles()

        when (option) {
            TransactionsDownloadOption.TransactionDetails -> {
                downloadBundleAfterCsvSave = false
                downloadTransactionDetails()
            }
            TransactionsDownloadOption.FarmGeolocations -> downloadFarmGeolocations()
            TransactionsDownloadOption.Both -> {
                downloadBundleAfterCsvSave = true
                downloadTransactionDetails()
            }
        }
    }

    private fun downloadTransactionDetails() {
        setEffect(TransactionsContract.Effect.ToggleDownloadLoader(true))

        launch {
            getInteractor(currentTab).downloadTransactionListCSV(filter.forTab(currentTab))
                .onSuccess { body ->
                    if (body == null) {
                        setEffect(
                            TransactionsContract.Effect.ToggleDownloadLoader(false),
                            TransactionsContract.Effect.ShowMessage(resourceProvider.getString(R.string.download_failed))
                        )
                        return@onSuccess
                    }

                    downloadedCSV = body
                    downloadedFileName = "Whimo-transactions.csv"

                    setEffect(
                        TransactionsContract.Effect.ToggleDownloadLoader(false),
                        TransactionsContract.Effect.CSVDataDownloaded(downloadedFileName)
                    )
                }
                .onError {
                    setEffect(
                        TransactionsContract.Effect.ToggleDownloadLoader(false),
                        TransactionsContract.Effect.ShowMessage(errorHandler.parseError(it))
                    )
                }
        }
    }

    private fun downloadFarmGeolocations() {
        setEffect(TransactionsContract.Effect.ToggleDownloadLoader(true))

        launch {
            getInteractor(currentTab).downloadTransactionListLocationBundle(filter.forTab(currentTab))
                .onSuccess { body ->
                    if (body == null) {
                        setEffect(
                            TransactionsContract.Effect.ToggleDownloadLoader(false),
                            TransactionsContract.Effect.ShowMessage(resourceProvider.getString(R.string.download_failed))
                        )
                        return@onSuccess
                    }

                    downloadedBundle = body
                    downloadedFileName = "Whimo-farm-geolocations.zip"

                    setEffect(
                        TransactionsContract.Effect.ToggleDownloadLoader(false),
                        TransactionsContract.Effect.ZipDataDownloaded(downloadedFileName)
                    )
                }
                .onError {
                    setEffect(
                        TransactionsContract.Effect.ToggleDownloadLoader(false),
                        TransactionsContract.Effect.ShowMessage(errorHandler.parseError(it))
                    )
                }
        }
    }

    private fun saveDownloadedFile(context: Context, uri: Uri) {
        setEffect(TransactionsContract.Effect.ToggleDownloadLoader(true))

        launch {
            var inputStream: InputStream? = null
            val savingCsv = downloadedCSV != null
            var saved = false

            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    inputStream = downloadedCSV?.byteStream() ?: downloadedBundle?.byteStream()
                    inputStream?.copyTo(outputStream)
                    outputStream.flush()
                    saved = inputStream != null
                }

                if (saved) {
                    setEffect(TransactionsContract.Effect.ShowMessage(resourceProvider.getString(R.string.file_saved, downloadedFileName)))
                } else {
                    setEffect(TransactionsContract.Effect.ShowMessage(resourceProvider.getString(R.string.file_save_failed)))
                }
            } catch (e: Exception) {
                setEffect(TransactionsContract.Effect.ShowMessage(resourceProvider.getString(R.string.file_save_failed)))
            } finally {
                inputStream?.close()
                if (savingCsv) {
                    downloadedCSV = null
                } else {
                    downloadedBundle = null
                }
                setEffect(TransactionsContract.Effect.ToggleDownloadLoader(false))
            }

            if (savingCsv && downloadBundleAfterCsvSave) {
                downloadBundleAfterCsvSave = false
                if (saved) {
                    downloadFarmGeolocations()
                }
            }
        }
    }

    private fun clearDownloadedFiles() {
        downloadedCSV?.close()
        downloadedBundle?.close()
        downloadedCSV = null
        downloadedBundle = null
        downloadedFileName = ""
        downloadBundleAfterCsvSave = false
    }

    private fun networkAvailabilityChanged(isAvailable: Boolean) {
        val wasAvailable = networkAvailable
        networkAvailable = isAvailable

        if (wasAvailable == false && isAvailable) {
            syncPendingTransactions(refreshAfterSync = true)
        }
    }

    private fun showPendingTransactions() {
        launch {
            val pendingItems = createTransactionInteractor.getPendingTransactions()

            pendingTransactions[TransactionsTab.All] = pendingItems
            pendingTransactions[TransactionsTab.Bought] = pendingItems.filter { it.transactionModel.action == TransactionAction.Buying }
            pendingTransactions[TransactionsTab.Sold] = pendingItems.filter { it.transactionModel.action == TransactionAction.Selling }
            pendingTransactions[TransactionsTab.Converted] = emptyList()

            delay(100)
            updateTabs()
        }
    }

    private fun syncPendingTransactions(refreshAfterSync: Boolean) {
        if (pendingSyncJob?.isActive == true) return

        pendingSyncJob = launch {
            var syncStarted = false

            try {
                createTransactionInteractor.sendPendingTransactions { progress ->
                    syncStarted = true
                    updatePendingSyncProgress(progress)
                }
            } finally {
                if (syncStarted) {
                    updatePendingSyncProgress(null)
                    showPendingTransactions()
                    if (refreshAfterSync) {
                        refreshTransactions(currentTab)
                    }
                }
            }
        }
    }

    private fun updatePendingSyncProgress(progress: PendingTransactionsSyncProgress?) {
        updateBinding { b ->
            b.pendingSyncProgress = progress
        }
    }

    private fun getInteractor(tab: TransactionsTab): TransactionsInteractor {
        return when (tab) {
            TransactionsTab.All -> allTabInteractor
            TransactionsTab.Bought -> boughtTabInteractor
            TransactionsTab.Sold -> soldTabInteractor
            TransactionsTab.Converted -> convertedTabInteractor
        }
    }

    private fun collectHarvestSeasons(commodityGroup: CommodityGroupModel?): List<HarvestSeasonModel> {
        return tabStates.values
            .flatMap { state ->
                when (state) {
                    is TransactionsState.End -> state.transactions
                    is TransactionsState.PageLoading -> state.transactions
                    is TransactionsState.Success -> state.transactions
                    TransactionsState.Empty,
                    is TransactionsState.Error,
                    TransactionsState.Reloading -> emptyList()
                }
            }
            .filter { transaction ->
                commodityGroup == null || transaction.commodity.group?.id == commodityGroup.id
            }
            .mapNotNull { it.harvestSeason }
            .distinctBy { it.id }
            .sortedWith(
                compareBy<HarvestSeasonModel> { it.status?.ordinal ?: Int.MAX_VALUE }
                    .thenByDescending { it.startDate ?: LocalDate.MIN }
                    .thenBy { it.name }
            )
    }
}

private fun CommodityGroupModel?.commodityIdsOrNull(): List<String>? {
    return this?.commodities
        ?.map { it.id }
        ?.distinct()
        ?.sorted()
}

private fun CommodityGroupModel?.sameGroupAs(other: CommodityGroupModel?): Boolean {
    if (this == null && other == null) return true
    return this?.id == other?.id
}

private fun CommodityGroupModel?.refreshFrom(groups: List<CommodityGroupModel>): CommodityGroupModel? {
    return this?.let { selected -> groups.find { it.id == selected.id } ?: selected }
}

private fun TransactionsFilter.forTab(tab: TransactionsTab): TransactionsFilter {
    return when (tab) {
        TransactionsTab.All -> copy(groupConversions = true)
        TransactionsTab.Bought -> copy(
            action = TransactionAction.Buying,
            groupConversions = true,
        )
        TransactionsTab.Sold -> copy(
            action = TransactionAction.Selling,
            groupConversions = true,
        )
        TransactionsTab.Converted -> copy(
            action = null,
            type = TransactionType.Conversion,
            groupConversions = true,
        )
    }
}
