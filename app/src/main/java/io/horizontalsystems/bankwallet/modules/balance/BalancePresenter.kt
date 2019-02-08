package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import java.math.BigDecimal

class BalancePresenter(
        private var interactor: BalanceModule.IInteractor,
        private val router: BalanceModule.IRouter,
        private val dataSource: BalanceModule.BalanceItemDataSource,
        private val factory: BalanceViewItemFactory) : BalanceModule.IViewDelegate, BalanceModule.IInteractorDelegate {

    var view: BalanceModule.IView? = null

    //
    // BalanceModule.IViewDelegate
    //
    override val itemsCount: Int
        get() = dataSource.count


    override fun viewDidLoad() {
        interactor.initAdapters()
    }

    override fun getViewItem(position: Int) =
            factory.createViewItem(dataSource.getItem(position), dataSource.currency)

    override fun getHeaderViewItem() =
            factory.createHeaderViewItem(dataSource.items, dataSource.currency)

    override fun refresh() {
        interactor.refresh()
    }

    override fun onReceive(position: Int) {
        router.openReceiveDialog(dataSource.getItem(position).coin.code)
    }

    override fun onPay(position: Int) {
        router.openSendDialog(dataSource.getItem(position).coin.code)
    }

    override fun onClear() {
        interactor.clear()
    }

    //
    // BalanceModule.IInteractorDelegate
    //
    override fun didUpdateAdapters(adapters: List<IAdapter>) {
        dataSource.reset(adapters.map { BalanceModule.BalanceItem(it.coin, it.balance, it.state) })
        dataSource.currency?.let {
            interactor.fetchRates(it.code, dataSource.coinCodes)
        }

        view?.reload()
    }

    override fun didUpdateCurrency(currency: Currency) {
        dataSource.currency = currency
        dataSource.clearRates()
        interactor.fetchRates(currency.code, dataSource.coinCodes)
        view?.reload()
    }

    override fun didUpdateBalance(coinCode: CoinCode, balance: BigDecimal) {
        val position = dataSource.getPosition(coinCode)
        dataSource.setBalance(position, balance)
        view?.updateItem(position)
        view?.updateHeader()
    }

    override fun didUpdateState(coinCode: String, state: AdapterState) {
        val position = dataSource.getPosition(coinCode)
        dataSource.setState(position, state)
        view?.updateItem(position)
        view?.updateHeader()
    }

    override fun didUpdateRate(rate: Rate) {
        val position = dataSource.getPosition(rate.coinCode)
        dataSource.setRate(position, rate)
        view?.updateItem(position)
        view?.updateHeader()
    }

    override fun didRefresh() {
        view?.didRefresh()
    }

    override fun openManageCoins() {
        router.openManageCoins()
    }

}
