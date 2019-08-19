package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import java.math.BigDecimal

object BalanceModule {

    interface IView {
        fun didRefresh()
        fun reload()
        fun updateItem(position: Int)
        fun updateHeader()
        fun enabledCoinsCount(size: Int)
        fun setSortingOn(isOn: Boolean)
        fun showBackupAlert()
    }

    interface IViewDelegate {
        val itemsCount: Int

        fun viewDidLoad()
        fun getViewItem(position: Int): BalanceViewItem
        fun getHeaderViewItem(): BalanceHeaderViewItem
        fun refresh()
        fun onReceive(position: Int)
        fun onPay(position: Int)
        fun openManageCoins()
        fun onClear()
        fun onSortClick()
        fun onSortTypeChanged(sortType: BalanceSortType)
        fun openManageKeys()
    }

    interface IInteractor {
        fun refresh()
        fun initAdapters()
        fun fetchRates(currencyCode: String, coinCodes: List<CoinCode>)
        fun getSortingType(): BalanceSortType
        fun clear()
        fun saveSortingType(sortType: BalanceSortType)
    }

    interface IInteractorDelegate {
        fun didUpdateAdapters(adapters: List<IAdapter>)
        fun didUpdateCurrency(currency: Currency)
        fun didUpdateBalance(coinCode: CoinCode, balance: BigDecimal)
        fun didUpdateState(coinCode: String, state: AdapterState)
        fun didUpdateRate(rate: Rate)
        fun didRefresh()
        fun didEnabledCoinsCountUpdated(size: Int)
    }

    interface IRouter {
        fun openReceiveDialog(coin: String)
        fun openSendDialog(coin: String)
        fun openManageCoins()
        fun openSortTypeDialog(sortingType: BalanceSortType)
        fun openManageKeys()
    }

    class BalanceItemDataSource {
        private var updatedPositions = mutableListOf<Int>()

        val count
            get() = items.count()

        var currency: Currency? = null

        val coinCodes: List<CoinCode>
            get() = items.map { it.coin.code }

        private var originalItems = listOf<BalanceItem>()
        var items = listOf<BalanceItem>()
        var balanceSortType: BalanceSortType = BalanceSortType.Name

        @Synchronized
        fun addUpdatedPosition(position: Int) {
            updatedPositions.add(position)
        }

        @Synchronized
        fun clearUpdatedPositions() {
            updatedPositions.clear()
        }

        fun set(items: List<BalanceItem>) {
            clearUpdatedPositions()
            originalItems = items
            sortBy(BalanceSortType.Name)
        }

        @Synchronized
        fun getUpdatedPositions(): List<Int> = updatedPositions.distinct()

        fun getItem(position: Int): BalanceItem = items[position]

        fun getPosition(coinCode: CoinCode): Int {
            return items.indexOfFirst { it.coin.code == coinCode }
        }

        fun setBalance(position: Int, balance: BigDecimal) {
            items[position].balance = balance
        }

        fun setState(position: Int, state: AdapterState) {
            items[position].state = state
        }

        fun setRate(position: Int, rate: Rate) {
            items[position].rate = rate
        }

        fun clearRates() {
            items.forEach {
                it.rate = null
            }
        }

        fun sortBy(sortType: BalanceSortType) {
            balanceSortType = sortType
            items = when (balanceSortType) {
                BalanceSortType.Value -> {
                    originalItems.sortedByDescending { it.fiatValue }
                }
                BalanceSortType.Name ->{
                    originalItems.sortedBy { it.coin.title }
                }
            }

        }

    }

    data class BalanceItem(
            val coin: Coin,
            var balance: BigDecimal = BigDecimal.ZERO,
            var state: AdapterState = AdapterState.NotSynced,
            var rate: Rate? = null,
            var isBackedUp: Boolean = false
    ) {
        val fiatValue: BigDecimal?
            get() = rate?.let { balance.times(it.value) }
    }

    fun init(view: BalanceViewModel, router: IRouter) {
        val interactor = BalanceInteractor(App.adapterManager, App.rateStorage, App.enabledWalletsStorage, App.currencyManager, App.localStorage)
        val presenter = BalancePresenter(interactor, router, BalanceItemDataSource(), BalanceViewItemFactory())

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }

}
