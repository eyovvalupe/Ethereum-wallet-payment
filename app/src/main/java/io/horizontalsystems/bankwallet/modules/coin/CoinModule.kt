package io.horizontalsystems.bankwallet.modules.coin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.coinkit.models.CoinType
import java.math.BigDecimal

object CoinModule {

    class Factory(private val coinTitle: String, private val coinType: CoinType, private val coinCode: String, private val coinId: String?) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val currency = App.currencyManager.baseCurrency
            val rateFormatter = RateFormatter(currency)
            val service = CoinService(coinType, currency, App.xRateManager, App.chartTypeStorage, App.priceAlertManager, App.notificationManager, App.localStorage, App.marketFavoritesManager)
            return CoinViewModel(rateFormatter, service, coinCode, coinTitle, coinId, RateChartViewFactory(currency, App.numberFormatter), listOf(service)) as T
        }

    }

    data class CoinCodeWithValue(val coinCode: String, val value: BigDecimal)
}

data class MarketTickerViewItem(
        val title: String,
        val subtitle: String,
        val value: String,
        val subvalue: String,
){
    fun areItemsTheSame(other: MarketTickerViewItem): Boolean {
        return title == other.title && subtitle == other.subvalue
    }

    fun areContentsTheSame(other: MarketTickerViewItem): Boolean {
        return this == other
    }
}
