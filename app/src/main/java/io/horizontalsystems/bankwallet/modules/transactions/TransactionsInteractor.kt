package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.NetworkAvailabilityManager
import io.horizontalsystems.bankwallet.core.managers.RateManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class TransactionsInteractor(
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager,
        private val currencyManager: ICurrencyManager,
        private val rateManager: RateManager,
        private val networkAvailabilityManager: NetworkAvailabilityManager) : TransactionsModule.IInteractor {

    var delegate: TransactionsModule.IInteractorDelegate? = null

    private val disposables = CompositeDisposable()
    private val ratesDisposables = CompositeDisposable()
    private val lastBlockHeightDisposables = CompositeDisposable()
    private val transactionUpdatesDisposables = CompositeDisposable()
    private var requestedTimestamps = hashMapOf<String, Long>()

    override fun initialFetch() {
        onUpdateWallets()

        walletManager.walletsUpdatedSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    onUpdateWallets()
                }
                .let { disposables.add(it) }

        adapterManager.adapterCreationObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    onUpdateWallets()
                }
                .let { disposables.add(it) }

        currencyManager.baseCurrencyUpdatedSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    ratesDisposables.clear()
                    requestedTimestamps.clear()
                    delegate?.onUpdateBaseCurrency()
                }
                .let { disposables.add(it) }

        networkAvailabilityManager.networkAvailabilitySignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    if (networkAvailabilityManager.isConnected) {
                        delegate?.onConnectionRestore()
                    }
                }
                .let { disposables.add(it) }
    }

    override fun fetchRecords(fetchDataList: List<TransactionsModule.FetchData>) {
        if (fetchDataList.isEmpty()) {
            delegate?.didFetchRecords(mapOf())
            return
        }

        val flowables = mutableListOf<Single<Pair<Wallet, List<TransactionRecord>>>>()

        fetchDataList.forEach { fetchData ->
            val adapter = walletManager.wallets.find { it == fetchData.wallet }?.let {
                adapterManager.getAdapterForWallet(it)
            }

            val flowable = when (adapter) {
                null -> Single.just(Pair(fetchData.wallet, listOf()))
                else -> {
                    adapter.getTransactions(fetchData.from, fetchData.limit)
                            .map {
                                Pair(fetchData.wallet, it)
                            }
                }
            }

            flowables.add(flowable)
        }

        Single.zip(flowables) {
            val res = mutableMapOf<Wallet, List<TransactionRecord>>()
            it.forEach {
                it as Pair<Wallet, List<TransactionRecord>>
                res[it.first] = it.second
            }
            res.toMap()
        }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { records, t2 ->
                    delegate?.didFetchRecords(records)
                }
                .let { disposables.add(it) }
    }

    override fun setSelectedWallets(selectedWallets: List<Wallet>) {
        delegate?.onUpdateSelectedWallets(if (selectedWallets.isEmpty()) walletManager.wallets else selectedWallets)
    }

    override fun fetchLastBlockHeights() {
        lastBlockHeightDisposables.clear()

        walletManager.wallets.forEach { wallet ->
            adapterManager.getAdapterForWallet(wallet)?.let { adapter ->
                adapter.lastBlockHeightUpdatedFlowable
                        .throttleLast(3, TimeUnit.SECONDS)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe { onUpdateLastBlockHeight(wallet, adapter) }
                        .let { lastBlockHeightDisposables.add(it) }
            }
        }
    }

    override fun fetchRate(coin: Coin, timestamp: Long) {
        val baseCurrency = currencyManager.baseCurrency
        val currencyCode = baseCurrency.code
        val composedKey = coin.code + timestamp

        if (requestedTimestamps.containsKey(composedKey)) return

        requestedTimestamps[composedKey] = timestamp

        rateManager.rateValueObservable(coin.code, currencyCode, timestamp)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    delegate?.didFetchRate(it, coin, baseCurrency, timestamp)
                }, {
                    requestedTimestamps.remove(composedKey)
                })
                .let { ratesDisposables.add(it) }
    }

    override fun clear() {
        disposables.clear()
        lastBlockHeightDisposables.clear()
        ratesDisposables.clear()
        transactionUpdatesDisposables.clear()
    }

    private fun onUpdateLastBlockHeight(wallet: Wallet, adapter: IAdapter) {
        adapter.lastBlockHeight?.let { lastBlockHeight ->
            delegate?.onUpdateLastBlockHeight(wallet, lastBlockHeight)
        }
    }

    private fun onUpdateWallets() {
        if (walletManager.wallets.map { adapterManager.getAdapterForWallet(it) }.any { it == null}) return

        transactionUpdatesDisposables.clear()

        val walletsData = mutableListOf<Triple<Wallet, Int, Int?>>()
        walletManager.wallets.forEach { wallet ->
            adapterManager.getAdapterForWallet(wallet)?.let { adapter ->
                walletsData.add(Triple(wallet, adapter.confirmationsThreshold, adapter.lastBlockHeight))

                adapter.transactionRecordsFlowable
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe {
                            delegate?.didUpdateRecords(it, wallet)
                        }
                        .let { transactionUpdatesDisposables.add(it) }
            }
        }

        delegate?.onUpdateWalletsData(walletsData)

    }

}
