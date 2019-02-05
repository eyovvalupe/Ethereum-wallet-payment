package io.horizontalsystems.bankwallet.modules.balance

import android.os.Handler
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class BalanceInteractor(
        private val walletManager: IWalletManager,
        private val rateStorage: IRateStorage,
        private val currencyManager: ICurrencyManager,
        private val refreshTimeout: Double = 2.0
) : BalanceModule.IInteractor {

    var delegate: BalanceModule.IInteractorDelegate? = null

    private var disposables: CompositeDisposable = CompositeDisposable()
    private var walletDisposables: CompositeDisposable = CompositeDisposable()
    private var rateDisposables: CompositeDisposable = CompositeDisposable()

    override fun initWallets() {
        onUpdateWallets()

        disposables.add(walletManager.walletsUpdatedSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    onUpdateWallets()
                })

        onUpdateCurrency()

        disposables.add(currencyManager.baseCurrencyUpdatedSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    onUpdateCurrency()
                })
    }

    override fun fetchRates(currencyCode: String, coinCodes: List<CoinCode>) {
        rateDisposables.clear()

        coinCodes.forEach {
            rateDisposables.add(rateStorage.latestRateObservable(it, currencyCode)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe {
                        delegate?.didUpdateRate(it)
                    })
        }
    }

    private fun onUpdateCurrency() {
        delegate?.didUpdateCurrency(currencyManager.baseCurrency)
    }

    private fun onUpdateWallets() {
        val wallets = walletManager.wallets

        delegate?.didUpdateWallets(wallets)

        walletDisposables.clear()

        wallets.forEach { wallet ->
            delegate?.didUpdateBalance(wallet.coinCode, wallet.adapter.balance)
            delegate?.didUpdateState(wallet.coinCode, wallet.adapter.state)

            walletDisposables.add(wallet.adapter.balanceUpdatedSignal
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe {
                        delegate?.didUpdateBalance(wallet.coinCode, wallet.adapter.balance)
                    })

            walletDisposables.add(wallet.adapter.stateUpdatedSignal
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe { state ->
                        delegate?.didUpdateState(wallet.coinCode, wallet.adapter.state)
                    })
        }
    }

    override fun refresh() {
        walletManager.refreshWallets()

        Handler().postDelayed({
            delegate?.didRefresh()
        }, (refreshTimeout * 1000).toLong())
    }

}
