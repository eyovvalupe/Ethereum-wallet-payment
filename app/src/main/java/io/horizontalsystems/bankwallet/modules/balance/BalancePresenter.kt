package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.IPredefinedAccountTypeManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule.BalanceItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule.ChartInfoState
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.MarketInfo
import java.math.BigDecimal
import java.util.concurrent.Executors

class BalancePresenter(
        private val interactor: BalanceModule.IInteractor,
        private val router: BalanceModule.IRouter,
        private val sorter: BalanceModule.IBalanceSorter,
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager,
        private val factory: BalanceViewItemFactory,
        private val sortingOnThreshold: Int = 5
) : BalanceModule.IViewDelegate, BalanceModule.IInteractorDelegate {

    var view: BalanceModule.IView? = null

    private val executor = Executors.newSingleThreadExecutor()

    private var items = listOf<BalanceItem>()
    private var viewItems = mutableListOf<BalanceViewItem>()
    private var currency: Currency = interactor.baseCurrency
    private var sortType: BalanceSortType = interactor.sortType
    private var accountToBackup: Account? = null

    // IViewDelegate

    override fun onLoad() {
        executor.submit {
            interactor.subscribeToWallets()
            interactor.subscribeToBaseCurrency()

            handleUpdate(interactor.wallets)

            updateViewItems()
            updateHeaderViewItem()
        }
    }

    override fun onRefresh() {
        executor.submit {
            interactor.refresh()
        }
    }

    override fun onReceive(viewItem: BalanceViewItem) {
        val wallet = viewItem.wallet

        if (wallet.account.isBackedUp) {
            router.openReceive(wallet)
        } else {
            interactor.predefinedAccountType(wallet)?.let { predefinedAccountType ->
                accountToBackup = wallet.account
                view?.showBackupRequired(wallet.coin, predefinedAccountType)
            }
        }
    }

    override fun onPay(viewItem: BalanceViewItem) {
        router.openSend(viewItem.wallet)
    }

    override fun onChart(viewItem: BalanceViewItem) {
        router.openChart(viewItem.wallet.coin)
    }

    override fun onAddCoinClick() {
        router.openManageCoins()
    }

    override fun onSortClick() {
        router.openSortTypeDialog(sortType)
    }

    override fun onSortTypeChange(sortType: BalanceSortType) {
        executor.submit {
            this.sortType = sortType
            interactor.saveSortType(sortType)

            updateViewItems()
        }
    }

    override fun onBackupClick() {
        accountToBackup?.let { account ->
            val accountType = predefinedAccountTypeManager.allTypes.first { it.supports(account.type) }
            router.openBackup(account, accountType.coinCodes)
            accountToBackup = null
        }
    }

    override fun onClear() {
        interactor.clear()
    }

    // IInteractorDelegate

    override fun didUpdateWallets(wallets: List<Wallet>) {
        executor.submit {
            handleUpdate(wallets)

            updateViewItems()
            updateHeaderViewItem()
        }
    }

    override fun didPrepareAdapters() {
        executor.submit {
            handleAdaptersReady()

            updateViewItems()
            updateHeaderViewItem()
        }
    }

    override fun didUpdateBalance(wallet: Wallet, balance: BigDecimal, balanceLocked: BigDecimal) {
        executor.submit {
            updateItem(wallet) { item ->
                item.balance = balance
                item.balanceLocked = balanceLocked
            }

            updateHeaderViewItem()
        }
    }

    override fun didUpdateState(wallet: Wallet, state: AdapterState) {
        executor.submit {
            updateItem(wallet) { item ->
                item.state = state
            }

            updateHeaderViewItem()
        }
    }

    override fun didUpdateCurrency(currency: Currency) {
        executor.submit {
            this.currency = currency

            handleRates()
            handleStats()

            updateViewItems()
            updateHeaderViewItem()
        }
    }

    override fun didUpdateMarketInfo(marketInfo: Map<String, MarketInfo>) {
        executor.submit {
            items.forEachIndexed { index, item ->
                marketInfo[item.wallet.coin.code]?.let {
                    item.marketInfo = it
                    viewItems[index] = factory.viewItem(item, currency)
                }
            }
            view?.set(viewItems)
            updateHeaderViewItem()
        }
    }

    override fun didUpdateChartInfo(chartInfo: ChartInfo, coinCode: String) {
        executor.submit {
            updateChartInfo(ChartInfoState.Loaded(chartInfo), coinCode)
        }
    }

    override fun didFailChartInfo(coinCode: String) {
        executor.submit {
            updateChartInfo(ChartInfoState.Failed, coinCode)
        }
    }

    override fun didRefresh() {
        view?.didRefresh()
    }

    private fun handleUpdate(wallets: List<Wallet>) {
        items = wallets.map { BalanceItem(it) }

        handleAdaptersReady()
        handleRates()
        handleStats()

        view?.set(sortIsOn = items.size >= sortingOnThreshold)
    }

    private fun handleAdaptersReady() {
        interactor.subscribeToAdapters(items.map { it.wallet })

        items.forEach { item ->
            item.balance = interactor.balance(item.wallet)
            item.balanceLocked = interactor.balanceLocked(item.wallet)
            item.state = interactor.state(item.wallet)
        }
    }

    private fun handleRates() {
        interactor.subscribeToMarketInfo(currency.code)

        items.forEach { item ->
            item.marketInfo = interactor.marketInfo(item.wallet.coin.code, currency.code)
        }
    }

    private fun handleStats() {
        interactor.subscribeToChartInfo(items.map { it.wallet.coin.code }, currency.code)

        items.forEach { item ->
            item.chartInfoState =
                    interactor.chartInfo(item.wallet.coin.code, currency.code)?.let {
                        ChartInfoState.Loaded(it)
                    } ?: ChartInfoState.Loading
        }
    }

    private fun updateItem(wallet: Wallet, updateBlock: (BalanceItem) -> Unit) {
        val index = items.indexOfFirst { it.wallet == wallet }
        if (index == -1)
            return

        val item = items[index]
        updateBlock(item)
        viewItems[index] = factory.viewItem(item, currency)

        view?.set(viewItems)
    }

    private fun updateViewItems() {
        items = sorter.sort(items, sortType)

        viewItems = items.map { factory.viewItem(it, currency) }.toMutableList()

        view?.set(viewItems)
    }

    private fun updateHeaderViewItem() {
        val headerViewItem = factory.headerViewItem(items, currency)
        view?.set(headerViewItem)
    }

    private fun updateChartInfo(chartInfoState: ChartInfoState, coinCode: String) {
        items.forEachIndexed { index, item ->
            if (item.wallet.coin.code == coinCode) {
                item.chartInfoState = chartInfoState
                viewItems[index] = factory.viewItem(item, currency)
            }
        }
        view?.set(viewItems)
    }


}
