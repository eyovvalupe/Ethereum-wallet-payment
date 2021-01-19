package io.horizontalsystems.bankwallet.modules.ratechart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.ChartType
import io.horizontalsystems.xrateskit.entities.MarketInfo
import java.math.BigDecimal

object RateChartModule {

    interface View {
        fun showSpinner()
        fun hideSpinner()
        fun setChartType(type: ChartType)
        fun showChartInfo(viewItem: ChartInfoViewItem)
        fun showMarketInfo(viewItem: MarketInfoViewItem)
        fun showSelectedPointInfo(item: ChartPointViewItem)
        fun showError(ex: Throwable)
        fun setEmaEnabled(enabled: Boolean)
        fun setMacdEnabled(enabled: Boolean)
        fun setRsiEnabled(enabled: Boolean)
        fun notificationIconUpdated()
        fun openNotificationMenu(coinId: String, coinName: String)
        fun setIsFavorite(value: Boolean)
    }

    interface ViewDelegate {
        fun viewDidLoad()
        fun onSelect(type: ChartType)
        fun onTouchSelect(point: PointInfo)
        fun toggleEma()
        fun toggleMacd()
        fun toggleRsi()
        fun onNotificationClick()
        fun onFavoriteClick()
        fun onUnfavoriteClick()
    }

    interface Interactor {
        var defaultChartType: ChartType?
        val notificationsAreEnabled: Boolean

        fun getMarketInfo(coinCode: String, currencyCode: String): MarketInfo?
        fun getChartInfo(coinCode: String, currencyCode: String, chartType: ChartType): ChartInfo?
        fun observeChartInfo(coinCode: String, currencyCode: String, chartType: ChartType)
        fun observeMarketInfo(coinCode: String, currencyCode: String)
        fun clear()
        fun observeAlertNotification(coinCode: String)
        fun getPriceAlert(coinCode: String): PriceAlert
        fun isCoinFavorite(coinCode: String, coinType: CoinType?): Boolean
        fun favorite(coinCode: String, coinType: CoinType?)
        fun unfavorite(coinCode: String, coinType: CoinType?)
    }

    interface InteractorDelegate {
        fun onUpdate(chartInfo: ChartInfo)
        fun onUpdate(marketInfo: MarketInfo)
        fun onError(ex: Throwable)
        fun updateAlertNotificationIconState()
        fun updateFavoriteNotificationItemState()
    }

    interface Router

    class Factory(private val coinTitle: String, private val coinCode: String, private val coinId: String?, private val coinType: CoinType?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val currency = App.currencyManager.baseCurrency
            val rateFormatter = RateFormatter(currency)

            val view = RateChartView()
            val interactor = RateChartInteractor(App.xRateManager, App.chartTypeStorage, App.priceAlertManager, App.notificationManager, App.localStorage, App.marketFavoritesManager)
            val presenter = RateChartPresenter(view, rateFormatter, interactor, coinCode, coinTitle, coinId, coinType, currency, RateChartViewFactory())

            interactor.delegate = presenter

            return presenter as T
        }
    }

    data class CoinCodeWithValue(val coinCode: String, val value: BigDecimal)
}
