package io.horizontalsystems.bankwallet.modules.balance

import android.content.Context
import androidx.core.content.ContextCompat
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.xrateskit.entities.MarketInfo
import java.math.BigDecimal
import java.math.RoundingMode

data class BalanceViewItem(
        val wallet: Wallet,
        val coinCode: String,
        val coinTitle: String,
        val coinType: String?,
        val coinValue: DeemedValue,
        val exchangeValue: DeemedValue,
        val diff: RateDiff,
        val fiatValue: DeemedValue,
        val coinValueLocked: DeemedValue,
        val fiatValueLocked: DeemedValue,
        val expanded: Boolean,
        val sendEnabled: Boolean = false,
        val receiveEnabled: Boolean = false,
        val syncingData: SyncingData?,
        val failedIconVisible: Boolean,
        val coinIconVisible: Boolean,
        val coinTypeLabelVisible: Boolean,
        var hideBalance: Boolean
)

data class RateDiff(
        val deemedValue: DeemedValue,
        val positive: Boolean
)

data class BalanceHeaderViewItem(val currencyValue: CurrencyValue?, val upToDate: Boolean) {

    val xBalanceText = currencyValue?.let {
        App.numberFormatter.formatFiat(it.value, it.currency.symbol, 2, 2)
    }

    fun getBalanceTextColor(context: Context) = ContextCompat.getColor(context, if (upToDate) R.color.yellow_d else R.color.yellow_50)

}

data class DeemedValue(val text: String?, val dimmed: Boolean = false, val visible: Boolean = true)
sealed class SyncingData {
    data class Blockchain(val progress: Int?, val spinnerProgress: Int, val until: String?, val syncingTextVisible: Boolean) : SyncingData()
    data class SearchingTxs(val txCount: Int, val syncingTextVisible: Boolean) : SyncingData()
}

class BalanceViewItemFactory {

    private val diffScale = 2

    private fun coinValue(state: AdapterState?, balance: BigDecimal?, coin: Coin, visible: Boolean): DeemedValue {
        val dimmed = state !is AdapterState.Synced
        val value = balance?.let {
            val maxFraction = if (it < BigDecimal("0.0001")) 8 else 4
            App.numberFormatter.formatCoin(balance, coin.code, 0, maxFraction)
        }

        return DeemedValue(value, dimmed, visible)
    }

    private fun currencyValue(state: AdapterState?, balance: BigDecimal?, currency: Currency, marketInfo: MarketInfo?, visible: Boolean): DeemedValue {
        val dimmed = state !is AdapterState.Synced || marketInfo?.isExpired() ?: false
        val value = marketInfo?.rate?.let { rate ->
            balance?.let {
                App.numberFormatter.formatFiat(it * rate, currency.symbol, 0, 2)
            }
        }

        return DeemedValue(value, dimmed, visible)
    }

    private fun rateValue(currency: Currency, marketInfo: MarketInfo?): DeemedValue {
        var dimmed = false
        val value = marketInfo?.let {
            dimmed = marketInfo.isExpired()
            App.numberFormatter.formatFiat(marketInfo.rate, currency.symbol, 2, 4)
        }

        return DeemedValue(value, dimmed = dimmed)
    }

    private fun syncingData(state: AdapterState?, expanded: Boolean): SyncingData? {
        return when (state) {
            is AdapterState.Syncing -> {
                if (state.lastBlockDate != null) {
                    SyncingData.Blockchain(state.progress, state.progress, DateHelper.formatDate(state.lastBlockDate, "MMM d, yyyy"), !expanded)
                } else {
                    SyncingData.Blockchain(null, state.progress, null, !expanded)
                }
            }
            is AdapterState.SearchingTxs -> SyncingData.SearchingTxs(state.count, !expanded)
            else -> null
        }

    }

    private fun coinTypeLabelVisible(coinType: CoinType): Boolean {
        return coinType.typeLabel() != null
    }

    fun viewItem(item: BalanceModule.BalanceItem, currency: Currency, expanded: Boolean, hideBalance: Boolean): BalanceViewItem {
        val wallet = item.wallet
        val coin = wallet.coin
        val state = item.state
        val marketInfo = item.marketInfo

        val syncing = state is AdapterState.Syncing || state is AdapterState.SearchingTxs
        val balanceTotalVisibility = item.balanceTotal != null && (!syncing || expanded)
        val balanceLockedVisibility = item.balanceLocked != null

        val rateDiff = getRateDiff(item)

        return BalanceViewItem(
                wallet = item.wallet,
                coinCode = coin.code,
                coinTitle = coin.title,
                coinType = coin.type.typeLabel(),
                coinValue = coinValue(state, item.balanceTotal, coin, balanceTotalVisibility),
                coinValueLocked = coinValue(state, item.balanceLocked, coin, balanceLockedVisibility),
                fiatValue = currencyValue(state, item.balanceTotal, currency, marketInfo, balanceTotalVisibility),
                fiatValueLocked = currencyValue(state, item.balanceLocked, currency, marketInfo, balanceLockedVisibility),
                exchangeValue = rateValue(currency, marketInfo),
                diff = rateDiff,
                expanded = expanded,
                sendEnabled = state is AdapterState.Synced,
                receiveEnabled = state != null,
                syncingData = syncingData(state, expanded),
                failedIconVisible = state is AdapterState.NotSynced,
                coinIconVisible = state !is AdapterState.NotSynced,
                coinTypeLabelVisible = coinTypeLabelVisible(coin.type),
                hideBalance = hideBalance
        )
    }

    private fun getRateDiff(item: BalanceModule.BalanceItem): RateDiff {
        val scaledValue = item.marketInfo?.diff?.setScale(diffScale, RoundingMode.HALF_EVEN)?.stripTrailingZeros()
        val isPositive = (scaledValue ?: BigDecimal.ZERO) >= BigDecimal.ZERO
        val rateDiffText = scaledValue?.let { App.numberFormatter.format(scaledValue.abs(), 0, diffScale, suffix = "%") }
        val dimmed = item.marketInfo?.isExpired() ?: true
        return RateDiff(DeemedValue(rateDiffText, dimmed, true), isPositive)
    }

    fun headerViewItem(items: List<BalanceModule.BalanceItem>, currency: Currency): BalanceHeaderViewItem {
        var total = BigDecimal.ZERO
        var upToDate = true

        items.forEach { item ->
            val balanceTotal = item.balanceTotal
            val marketInfo = item.marketInfo

            if (balanceTotal != null && marketInfo != null) {
                total += balanceTotal.multiply(marketInfo.rate)

                upToDate = !marketInfo.isExpired()
            }

            if (item.state == null || item.state != AdapterState.Synced) {
                upToDate = false
            }
        }

        return BalanceHeaderViewItem(CurrencyValue(currency, total), upToDate)
    }

}
