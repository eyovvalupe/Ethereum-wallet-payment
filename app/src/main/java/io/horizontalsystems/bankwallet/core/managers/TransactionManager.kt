package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.reactivex.disposables.CompositeDisposable

class TransactionManager(
        private val storage: ITransactionRecordStorage,
        private val rateSyncer: ITransactionRateSyncer,
        private val walletManager: IWalletManager,
        private val currencyManager: ICurrencyManager,
        wordsManager: IWordsManager,
        networkAvailabilityManager: NetworkAvailabilityManager) {

    private val disposables: CompositeDisposable = CompositeDisposable()
    private var adapterDisposables: CompositeDisposable = CompositeDisposable()

    init {
        resubscribeToAdapters()

        disposables.add(walletManager.walletsUpdatedSignal
                .subscribe {
                    resubscribeToAdapters()
                })

        disposables.add(currencyManager.baseCurrencyUpdatedSignal
                .subscribe{
                    handleCurrencyChange()
                })

        disposables.add(networkAvailabilityManager.networkAvailabilitySignal
                .subscribe {
                    if (networkAvailabilityManager.isConnected) {
                        syncRates()
                    }
                })
    }

    fun clear() {
        storage.deleteAll()
    }

    private fun resubscribeToAdapters() {
        adapterDisposables.clear()

        walletManager.wallets.forEach { wallet ->
            adapterDisposables.add(wallet.adapter.transactionRecordsSubject
                    .subscribe { records ->
                        handle(records, wallet.coinCode)
                    })
        }
    }

    private fun handle(records: List<TransactionRecord>, coinCode: String) {
        records.forEach { record ->
            record.coinCode = coinCode
        }

        storage.update(records)
        syncRates()
    }

    private fun handleCurrencyChange() {
        storage.clearRates()
        syncRates()
    }

    private fun syncRates() {
        rateSyncer.sync(currencyManager.baseCurrency.code)
    }

}
