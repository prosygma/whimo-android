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
package com.whimo.presentation.transactions.transactiondetails

import com.whimo.R
import com.whimo.base.BaseViewModel
import com.whimo.base.CoreViewEvent
import com.whimo.data.base.common.onError
import com.whimo.data.base.common.onSuccess
import com.whimo.domain.commodity.CommodityInteractor
import com.whimo.domain.commodity.refreshBalances
import com.whimo.domain.commodity.models.CommodityBalanceFilter
import com.whimo.domain.geodata.GeoDataInteractor
import com.whimo.domain.harvestseasons.models.HarvestSeasonStatus
import com.whimo.domain.harvestseasons.models.getYearRangeText
import com.whimo.domain.settings.models.AccountModel
import com.whimo.domain.transactions.TransactionDetailsInteractor
import com.whimo.domain.transactions.models.TraceabilityCountsModel
import com.whimo.domain.transactions.models.TransactionModel
import com.whimo.domain.transactions.models.TransactionStatus
import com.whimo.domain.transactions.models.TransactionType
import com.whimo.domain.transactions.models.getConversionInputs
import com.whimo.domain.transactions.models.getConversionOutputs
import com.whimo.domain.transactions.models.getCommodityText
import com.whimo.domain.transactions.models.getCommodityVolumeText
import com.whimo.domain.transactions.models.getCommodityVolumeTextWithSign
import com.whimo.domain.transactions.models.isConversion
import com.whimo.extensions.toFormattedDateString
import com.whimo.extensions.toQuantityTextWithUnit
import com.whimo.network.ErrorHandler
import com.whimo.presentation.transactions.transactiondetails.components.PieChartItem
import com.whimo.presentation.ui.theme.ColorMidnightBlue
import com.whimo.presentation.ui.theme.ColorMulberryPurple
import com.whimo.presentation.ui.theme.ColorSeaBlue
import com.whimo.presentation.ui.theme.ColorSuccess
import com.whimo.providers.ResourceProvider
import com.whimo.providers.SharedPreferencesProvider

class TransactionDetailsViewModel(
    private val interactor: TransactionDetailsInteractor,
    private val commodityInteractor: CommodityInteractor,
    private val geoDataInteractor: GeoDataInteractor,
    private val resourceProvider: ResourceProvider,
    private val errorHandler: ErrorHandler,
    private val sharedPreferencesProvider: SharedPreferencesProvider,
) : BaseViewModel<TransactionDetailsContract.Binding>() {

    private var transactionId: String = ""
    private var transactionModel: TransactionModel? = null
    private var accountModel: AccountModel? = null
    private var traceabilityCounts: TraceabilityCountsModel? = null
    private var acceptBalance: Float? = null
    private var acceptBalanceKey: AcceptBalanceKey? = null
    private var isAcceptBalanceLoading = false
    private var statusUpdated = false

    override fun createBinding(): TransactionDetailsContract.Binding {
        return TransactionDetailsContract.Binding()
    }

    override fun handleEvents(event: CoreViewEvent) {
        super.handleEvents(event)
        when (event) {
            is TransactionDetailsContract.Event.OnCreate -> onCreate(event.transactionModel)
            is TransactionDetailsContract.Event.Refresh -> refresh()
            is TransactionDetailsContract.Event.AddGeoDataClicked -> addGeoDataClicked()
            is TransactionDetailsContract.Event.AcceptTransaction -> acceptTransaction()
            is TransactionDetailsContract.Event.RejectTransaction -> rejectTransaction()
            is TransactionDetailsContract.Event.ResendNotification -> resendNotification()
            is TransactionDetailsContract.Event.OnInitialSuppliersHistoryClick -> onInitialSuppliersHistoryClick()
            is TransactionDetailsContract.Event.OnSuppliersHistoryClick -> onSuppliersHistoryClick(event.transaction)
        }
    }

    override fun copyBinding(binding: TransactionDetailsContract.Binding): TransactionDetailsContract.Binding {
        return binding.copy()
    }

    private fun onCreate(transaction: TransactionModel) {
        transactionId = transaction.id
        accountModel = sharedPreferencesProvider.getAccount()

        launch {
            transactionModel = interactor.getTransactionDetailsFromDB(transactionId)
            traceabilityCounts = interactor.getTraceabilityCountsFromDB(transactionId)

            if (transactionModel == null) transactionModel = transaction

            updateView()
            loadAcceptBalanceIfNeeded()
            updateChartItemsView()

            refresh()

            if (transactionModel?.isConversion() != true) {
                setEffect(TransactionDetailsContract.Effect.RefreshHistory(transactionModel!!))
            }
        }
    }

    private fun refresh() {
        getTransactionDetails()
        if (transactionModel?.isConversion() != true) {
            getTransactionTraceability()
        }
    }

    private fun getTransactionDetails() {
        launch {
            val detailsResult = if (transactionModel?.isConversion() == true) {
                val groupId = transactionModel?.groupId ?: transactionId
                interactor.getConversionTransactionDetails(groupId)
            } else {
                interactor.getTransactionDetails(transactionId)
            }

            detailsResult
                .onSuccess {
                    if (it != null) {
                        transactionModel = it
                        interactor.saveTransactionDetailsToDB(it)

                        if (statusUpdated) {
                            statusUpdated = false
                            setEffect(TransactionDetailsContract.Effect.RefreshHistory(it))
                        }

                        if (!it.isConversion()) {
                            getTransactionTraceability()
                        }

                        updateView()
                        loadAcceptBalanceIfNeeded()
                    }
                }
                .onError {
                    val errorMessage = errorHandler.parseError(it)
                }
        }
    }

    private fun getTransactionTraceability() {
        if (transactionModel?.isConversion() == true) return

        if (transactionModel?.traceability != null
            && (transactionModel?.status == TransactionStatus.Accepted
                    || transactionModel?.status == TransactionStatus.Recorded)) {
            launch {
                interactor.getTransactionTraceability(transactionId)
                    .onSuccess {
                        if (it != null) {
                            traceabilityCounts = it
                            interactor.saveTraceabilityCountsToDB(transactionId, it)

                            updateChartItemsView()
                        }
                    }
                    .onError {
                        val errorMessage = errorHandler.parseError(it)
                    }
            }
        }
    }

    private fun updateView() {
        updateBinding { b ->
            b.toolbarTitle = getToolbarTitle()
            b.commodityText = transactionModel?.getCommodityText()
            b.commodityVolume = transactionModel?.getCommodityVolumeTextWithSign()
            b.commodityWarningText = getCommodityWarningText()
            b.isConversion = transactionModel?.isConversion() == true
            b.conversionInputs = transactionModel?.getConversionInputs().orEmpty()
            b.conversionOutputs = transactionModel?.getConversionOutputs().orEmpty()
            b.harvestSeason = transactionModel?.harvestSeason

            b.showLocation = !b.isConversion &&
                    (transactionModel?.type == TransactionType.Producer || transactionModel?.isAutomatic == true)
            b.locationProvider = transactionModel?.locationProvider
            b.location = transactionModel?.location

            b.traceability = if (b.isConversion) null else transactionModel?.traceability

            b.accountId = accountModel?.id
            b.buyer = if (b.isConversion) null else transactionModel?.buyer
            b.seller = if (b.isConversion) null else transactionModel?.seller

            b.status = if (b.isConversion) null else transactionModel?.status

            b.createdDateText = transactionModel?.createdDate?.toFormattedDateString()
            b.expiryDateText = if (b.isConversion) null else transactionModel?.expiresDate?.toFormattedDateString()

            if (!b.isConversion && transactionModel?.status == TransactionStatus.Pending && accountModel != null) {
                val isInitiator = transactionModel?.createdById == accountModel!!.id
                b.showInitiatorActionButtons = isInitiator
                b.showRecipientActionButtons = !isInitiator
            } else {
                b.showInitiatorActionButtons = false
                b.showRecipientActionButtons = false
            }

            b.acceptBlocked = isAcceptBlockedByBalance()
            b.acceptEnabled = isAcceptEnabled()
            b.acceptBalanceLoading = isAcceptBalanceLoading
            b.acceptWarningText = getAcceptWarningText()
            b.acceptBlockedDialogText = getAcceptBlockedDialogText()
        }
    }

    private fun updateChartItemsView() {
        if (transactionModel?.isConversion() == true) {
            updateBinding { b ->
                b.chartItems = emptyList()
                b.downloadEnabled = false
                b.dialogDescription = ""
            }
            return
        }

        val chartItems = traceabilityCounts?.let {
            listOf(
                PieChartItem("Full", it.full, ColorSuccess),
                PieChartItem("Partial", it.partial, ColorMulberryPurple),
                PieChartItem("Conditional", it.conditional, ColorSeaBlue),
                PieChartItem("Incomplete", it.incomplete, ColorMidnightBlue),
            )
        } ?: emptyList()

        updateBinding { b ->
            b.chartItems = chartItems.filter { it.value > 0 }

            b.downloadEnabled = transactionModel?.traceability != null && transactionModel?.status == TransactionStatus.Accepted
            b.dialogDescription = resourceProvider.getString(
                R.string.downloading_transaction_details,
                chartItems.sumOf { it.value })
        }
    }

    private fun addGeoDataClicked() {
        transactionModel?.let {
            setEffect(TransactionDetailsContract.Effect.NavigateAddGeolocation(it))
        }
    }

    private fun acceptTransaction() {
        if (!isAcceptEnabled()) return
        if (isAcceptBlockedByBalance()) return

        launch {
            setEffect(TransactionDetailsContract.Effect.ToggleAcceptLoader(true))

            interactor.updateTransactionStatus(transactionId, TransactionStatus.Accepted.statusName)
                .onSuccess {
                    setEffect(
                        TransactionDetailsContract.Effect.ToggleAcceptLoader(false),
                        TransactionDetailsContract.Effect.StatusChanged
                    )
                    statusUpdated = true
                    refresh()
                }
                .onError {
                    val errorMessage = errorHandler.parseError(it)

                    setEffect(
                        TransactionDetailsContract.Effect.ToggleAcceptLoader(false),
                        TransactionDetailsContract.Effect.ShowMessage(errorMessage)
                    )
                }
        }
    }

    private fun rejectTransaction() {
        launch {
            setEffect(TransactionDetailsContract.Effect.ToggleRejectLoader(true))

            interactor.updateTransactionStatus(transactionId, TransactionStatus.Rejected.statusName)
                .onSuccess {
                    setEffect(
                        TransactionDetailsContract.Effect.ToggleRejectLoader(false),
                        TransactionDetailsContract.Effect.StatusChanged
                    )
                    refresh()
                }
                .onError {
                    val errorMessage = errorHandler.parseError(it)

                    setEffect(
                        TransactionDetailsContract.Effect.ToggleRejectLoader(false),
                        TransactionDetailsContract.Effect.ShowMessage(errorMessage)
                    )
                }
        }
    }

    private fun resendNotification() {
        launch {
            setEffect(TransactionDetailsContract.Effect.ToggleResendLoader(true))

            interactor.resendNotification(transactionId)
                .onSuccess {
                    setEffect(
                        TransactionDetailsContract.Effect.ToggleResendLoader(false),
                        TransactionDetailsContract.Effect.ShowMessage(resourceProvider.getString(R.string.notification_sent))
                    )
                }
                .onError {
                    val errorMessage = errorHandler.parseError(it)

                    setEffect(
                        TransactionDetailsContract.Effect.ToggleResendLoader(false),
                        TransactionDetailsContract.Effect.ShowMessage(errorMessage)
                    )
                }
        }
    }

    private fun onInitialSuppliersHistoryClick() {
        transactionModel?.let {
            setEffect(TransactionDetailsContract.Effect.NavigateInitialSupplierHistory(it))
        }
    }
    private fun onSuppliersHistoryClick(transaction: TransactionModel) {
        transactionModel?.let {
            setEffect(TransactionDetailsContract.Effect.NavigateInitialSupplierHistory(it))
        }
        setEffect(TransactionDetailsContract.Effect.NavigateSupplierHistory(transaction))
    }

    private fun getToolbarTitle(): String {
        if (transactionModel?.isConversion() == true) {
            return resourceProvider.getString(R.string.conversion_transaction)
        }

        return if (isCurrentUserSeller()) {
            resourceProvider.getString(R.string.sales_transaction)
        } else {
            resourceProvider.getString(R.string.transaction_details)
        }
    }

    private fun getCommodityWarningText(): String? {
        return if (isInsufficientAcceptBalance()) {
            resourceProvider.getString(R.string.not_enough_available_balance)
        } else {
            null
        }
    }

    private fun getAcceptWarningText(): String? {
        if (!isInsufficientAcceptBalance() || !isAutomaticTransactionAllowed()) return null

        return resourceProvider.getString(
            R.string.automatic_transaction_balance_message,
            getMissingVolumeText(),
        )
    }

    private fun getAcceptBlockedDialogText(): String {
        if (!isAcceptBlockedByBalance()) return ""

        val transaction = transactionModel ?: return ""
        val availableVolume = acceptBalance ?: return ""
        val seasonText = transaction.harvestSeason?.getYearRangeText().orEmpty()

        return resourceProvider.getString(
            R.string.accept_transaction_insufficient_balance_dialog,
            availableVolume.coerceAtLeast(0f).toQuantityTextWithUnit(transaction.commodity.unit),
            seasonText,
            transaction.getCommodityVolumeText(),
        )
    }

    private fun isAcceptBlockedByBalance(): Boolean {
        return isInsufficientAcceptBalance() && !isAutomaticTransactionAllowed()
    }

    private fun isAcceptEnabled(): Boolean {
        return !isAcceptBalanceLoading &&
                !isAcceptBlockedByBalance() &&
                (!requiresAcceptBalanceCheck() || acceptBalance != null)
    }

    private fun isInsufficientAcceptBalance(): Boolean {
        val transaction = transactionModel ?: return false
        val balance = acceptBalance ?: return false

        return requiresAcceptBalanceCheck() &&
                transaction.volume > 0 &&
                balance < transaction.volume
    }

    private fun requiresAcceptBalanceCheck(): Boolean {
        val transaction = transactionModel ?: return false

        return isCurrentUserSellerRecipient() &&
                transaction.status == TransactionStatus.Pending &&
                transaction.harvestSeason != null
    }

    private fun isCurrentUserSellerRecipient(): Boolean {
        return isCurrentUserSeller() &&
                transactionModel?.status == TransactionStatus.Pending &&
                transactionModel?.createdById != accountModel?.id
    }

    private fun isCurrentUserSeller(): Boolean {
        val accountId = accountModel?.id ?: return false
        val transaction = transactionModel ?: return false

        return transaction.seller?.id == accountId
    }

    private fun isAutomaticTransactionAllowed(): Boolean {
        return transactionModel?.harvestSeason?.status == HarvestSeasonStatus.Active
    }

    private fun getMissingVolumeText(): String {
        val transaction = transactionModel ?: return ""
        val balance = acceptBalance ?: 0f
        val missingVolume = (transaction.volume - balance).coerceAtLeast(0f)

        return missingVolume.toQuantityTextWithUnit(transaction.commodity.unit)
    }

    private fun loadAcceptBalanceIfNeeded() {
        val transaction = transactionModel
        val requestKey = transaction?.toAcceptBalanceKey()

        if (requestKey == null || !requiresAcceptBalanceCheck()) {
            resetAcceptBalance()
            return
        }

        if (acceptBalanceKey == requestKey && (isAcceptBalanceLoading || acceptBalance != null)) return

        acceptBalanceKey = requestKey
        acceptBalance = transaction.commodity.balance
        isAcceptBalanceLoading = true
        updateView()

        val requestFilter = CommodityBalanceFilter(
            groupId = transaction.commodity.group?.id,
            commodityId = transaction.commodity.id,
            harvestSeason = transaction.harvestSeason,
        )

        launch {
            commodityInteractor.refreshBalances(requestFilter) { cachedBalances ->
                val cachedBalance = cachedBalances
                    .firstOrNull { it.harvestSeason?.id == requestKey.harvestSeasonId }
                    ?.volume

                if (cachedBalance != null && acceptBalanceKey == requestKey) {
                    acceptBalance = cachedBalance
                    isAcceptBalanceLoading = false
                    updateView()
                }
            }
                .onSuccess { balances ->
                    if (acceptBalanceKey != requestKey) return@onSuccess

                    acceptBalance = balances
                        .orEmpty()
                        .firstOrNull { it.harvestSeason?.id == requestKey.harvestSeasonId }
                        ?.volume ?: 0f
                    isAcceptBalanceLoading = false
                    updateView()
                }
                .onError {
                    if (acceptBalanceKey != requestKey) return@onError

                    isAcceptBalanceLoading = false
                    val errorMessage = errorHandler.parseError(it)
                    setEffect(TransactionDetailsContract.Effect.ShowMessage(errorMessage))
                    updateView()
                }
        }
    }

    private fun resetAcceptBalance() {
        if (acceptBalanceKey == null && acceptBalance == null && !isAcceptBalanceLoading) return

        acceptBalanceKey = null
        acceptBalance = null
        isAcceptBalanceLoading = false
        updateView()
    }

    private fun TransactionModel.toAcceptBalanceKey(): AcceptBalanceKey? {
        val harvestSeasonId = harvestSeason?.id ?: return null
        return AcceptBalanceKey(
            commodityId = commodity.id,
            harvestSeasonId = harvestSeasonId,
        )
    }

    private data class AcceptBalanceKey(
        val commodityId: String,
        val harvestSeasonId: String,
    )
}
