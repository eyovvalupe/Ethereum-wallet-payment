package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class RateManager(private val storage: IRateStorage, private val networkManager: INetworkManager) {

    private var disposables: CompositeDisposable = CompositeDisposable()
    private var refreshDisposables: CompositeDisposable = CompositeDisposable()

    fun refreshLatestRates(coinCodes: List<String>, currencyCode: String) {
        refreshDisposables.clear()

        refreshDisposables.add(Flowable.mergeDelayError(
                coinCodes.map { coinCode ->
                    networkManager.getLatestRate(coinCode, currencyCode)
                            .map {
                                Rate(coinCode, currencyCode, it.value, it.timestamp, true)
                            }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({
                    storage.saveLatest(it)
                }, {

                }))
    }

    fun refreshZeroRates(currencyCode: String) {
        disposables.add(storage.zeroRatesObservable(currencyCode)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { rates ->
                    rates.forEach { rate ->
                        retrieveFromNetwork(rate.coinCode, rate.currencyCode, rate.timestamp)
                    }
                }
        )
    }

    fun rateValueObservable(coinCode: CoinCode, currencyCode: String, timestamp: Long): Flowable<BigDecimal> {
        return storage.rateObservable(coinCode, currencyCode, timestamp)
                .flatMap {
                    val rate = it.firstOrNull()

                    if (rate == null) {
                        storage.save(Rate(coinCode, currencyCode, BigDecimal.ZERO, timestamp, false))
                        retrieveFromNetwork(coinCode, currencyCode, timestamp)
                    }

                    if (rate != null && rate.value != BigDecimal.ZERO) {
                        Flowable.just(rate.value)
                    } else if (timestamp < ((System.currentTimeMillis() / 1000) - 3600)) {
                        Flowable.empty()
                    } else {
                        storage.latestRateObservable(coinCode, currencyCode)
                                .flatMap {
                                    if (it.expired) {
                                        Flowable.empty<BigDecimal>()
                                    } else {
                                        Flowable.just(it.value)
                                    }
                                }

                    }
                }
                .distinctUntilChanged()
    }

    private fun retrieveFromNetwork(coinCode: CoinCode, currencyCode: String, timestamp: Long) {
        disposables.add(networkManager.getRate(coinCode, currencyCode, timestamp)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { rateValue ->
                    storage.save(Rate(coinCode, currencyCode, rateValue, timestamp, false))
                })
    }

    fun clear() {
        storage.deleteAll()
    }

}
