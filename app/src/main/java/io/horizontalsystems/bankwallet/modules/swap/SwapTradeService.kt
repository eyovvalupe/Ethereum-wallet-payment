package io.horizontalsystems.bankwallet.modules.swap

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap.repositories.UniswapRepository
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.uniswapkit.models.TradeData
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.horizontalsystems.uniswapkit.models.TradeType
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.util.*


class SwapTradeService(
        private val uniswapRepository: UniswapRepository,
        coinFrom: Coin?
) {

    private var tradeDataDisposable: Disposable? = null

    //region internal subjects
    private val tradeTypeSubject = PublishSubject.create<TradeType>()
    private val coinFromSubject = PublishSubject.create<Optional<Coin>>()
    private val coinToSubject = PublishSubject.create<Optional<Coin>>()
    private val amountFromSubject = PublishSubject.create<Optional<BigDecimal>>()
    private val amountToSubject = PublishSubject.create<Optional<BigDecimal>>()
    private val stateSubject = PublishSubject.create<State>()
    private val tradeOptionsSubject = PublishSubject.create<TradeOptions>()
    //endregion

    //region outputs
    var coinFrom: Coin? = coinFrom
        private set(value) {
            field = value
            coinFromSubject.onNext(Optional.ofNullable(value))
        }
    val coinFromObservable: Observable<Optional<Coin>> = coinFromSubject

    var coinTo: Coin? = null
        private set(value) {
            field = value
            coinToSubject.onNext(Optional.ofNullable(value))
        }
    val coinToObservable: Observable<Optional<Coin>> = coinToSubject

    var amountFrom: BigDecimal? = null
        private set(value) {
            field = value
            amountFromSubject.onNext(Optional.ofNullable(value))
        }
    val amountFromObservable: Observable<Optional<BigDecimal>> = amountFromSubject

    var amountTo: BigDecimal? = null
        private set(value) {
            field = value
            amountToSubject.onNext(Optional.ofNullable(value))
        }
    val amountToObservable: Observable<Optional<BigDecimal>> = amountToSubject

    var tradeType: TradeType = TradeType.ExactIn
        private set(value) {
            field = value
            tradeTypeSubject.onNext(value)
        }
    val tradeTypeObservable: Observable<TradeType> = tradeTypeSubject

    var state: State = State.NotReady()
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }
    val stateObservable: Observable<State> = stateSubject

    var tradeOptions: TradeOptions = TradeOptions()
        set(value) {
            field = value
            tradeOptionsSubject.onNext(value)
            syncState()
        }
    val tradeOptionsObservable: Observable<TradeOptions> = tradeOptionsSubject

    @Throws
    fun transactionData(tradeData: TradeData): TransactionData {
        return uniswapRepository.transactionData(tradeData)
    }

    fun enterCoinFrom(coin: Coin?) {
        if (coinFrom == coin) return

        coinFrom = coin
        if (coinTo == coinFrom) {
            coinTo = null
            amountTo = null
        }
        syncState()
    }

    fun enterCoinTo(coin: Coin?) {
        if (coinTo == coin) return

        coinTo = coin
        if (coinFrom == coinTo) {
            coinFrom = null
            amountFrom = null
        }
        syncState()
    }

    fun enterAmountFrom(amount: BigDecimal?) {
        tradeType = TradeType.ExactIn

        if (amountsEqual(amountFrom, amount)) return

        amountFrom = amount
        amountTo = null
        syncState()

    }

    fun enterAmountTo(amount: BigDecimal?) {
        tradeType = TradeType.ExactOut

        if (amountsEqual(amountTo, amount)) return

        amountTo = amount
        amountFrom = null
        syncState()
    }

    fun switchCoins() {
        val swapCoin = coinTo
        coinTo = coinFrom

        enterCoinFrom(swapCoin)
    }
    //endregion

    private fun syncState() {
        val coinFrom = coinFrom
        val coinTo = coinTo
        val amount = if (tradeType == TradeType.ExactIn) amountFrom else amountTo

        if (coinFrom == null || coinTo == null || amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            state = State.NotReady()
        } else {
            state = State.Loading

            tradeDataDisposable?.dispose()
            tradeDataDisposable = null

            tradeDataDisposable = uniswapRepository.getTradeData(coinFrom, coinTo, amount, tradeType, tradeOptions)
                    .subscribeOn(Schedulers.io())
                    .subscribe({ tradeData ->
                        handle(tradeData)
                    }, { error ->
                        state = State.NotReady(listOf(error))
                    })
        }
    }

    private fun handle(tradeData: TradeData) {
        when (tradeData.type) {
            TradeType.ExactIn -> amountTo = tradeData.amountOut
            TradeType.ExactOut -> amountFrom = tradeData.amountIn
        }
        state = State.Ready(Trade(tradeData))
    }

    private fun amountsEqual(amount1: BigDecimal?, amount2: BigDecimal?): Boolean {
        return when {
            amount1 == null && amount2 == null -> true
            amount1 != null && amount2 != null && amount2.compareTo(amount1) == 0 -> true
            else -> false
        }
    }

    //region models
    sealed class State {
        object Loading : State()
        class Ready(val trade: Trade) : State()
        class NotReady(val errors: List<Throwable> = listOf()) : State()
    }

    enum class PriceImpactLevel {
        Normal, Warning, Forbidden
    }

    data class Trade(
            val tradeData: TradeData
    ) {
        val priceImpactLevel: PriceImpactLevel? = tradeData.priceImpact?.let {
            when {
                it >= BigDecimal.ZERO && it < warningPriceImpact -> PriceImpactLevel.Normal
                it >= warningPriceImpact && it < forbiddenPriceImpact -> PriceImpactLevel.Warning
                else -> PriceImpactLevel.Forbidden
            }
        }

    }
    //endregion

    companion object {
        private val warningPriceImpact = BigDecimal(1)
        private val forbiddenPriceImpact = BigDecimal(5)
    }

}
