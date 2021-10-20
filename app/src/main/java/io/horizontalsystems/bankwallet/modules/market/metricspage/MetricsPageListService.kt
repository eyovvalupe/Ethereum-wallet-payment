package io.horizontalsystems.bankwallet.modules.market.metricspage

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class MetricsPageListService(
    private val marketKit: MarketKit,
    private val currencyManager: ICurrencyManager
) : BackgroundManager.Listener, Clearable {

    sealed class State {
        object Loading : State()
        object Loaded : State()
        data class Error(val error: Throwable) : State()
    }

    private val dataUpdatedSubject = PublishSubject.create<Unit>()

    val stateObservable: BehaviorSubject<State> = BehaviorSubject.createDefault(State.Loading)

    var marketItems: List<MarketItem> = listOf()

    val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    private var topItemsDisposable: Disposable? = null
    private val disposable = CompositeDisposable()

    init {
        fetch()

        Observable.merge(dataUpdatedSubject, currencyManager.baseCurrencyUpdatedSignal)
                .subscribeIO {
                    marketItems = listOf()
                    fetch()
                }
                .let {
                    disposable.add(it)
                }
    }

    override fun clear() {
        disposable.clear()
    }

    override fun willEnterForeground() {
        dataUpdatedSubject.onNext(Unit)
    }

    fun refresh() {
        fetch()
    }

    private fun fetch() {
        topItemsDisposable?.let { disposable.remove(it) }

        stateObservable.onNext(State.Loading)

        topItemsDisposable = getAllMarketItemsAsync(currencyManager.baseCurrency)
                .subscribeIO({
                    marketItems = it
                    stateObservable.onNext(State.Loaded)
                }, {
                    stateObservable.onNext(State.Error(it))
                })

        topItemsDisposable?.let {
            disposable.add(it)
        }
    }

    private fun getAllMarketItemsAsync(currency: Currency): Single<List<MarketItem>> {
        return marketKit.marketInfosSingle(250)
            .map { coinMarkets ->
                coinMarkets.map { MarketItem.createFromCoinMarket(it, currency) }
            }
    }

}
