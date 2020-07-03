package io.horizontalsystems.bankwallet.modules.ratechart

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.Interactor
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.InteractorDelegate
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.View
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.ViewDelegate
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.ChartType
import io.horizontalsystems.xrateskit.entities.MarketInfo

class RateChartPresenter(
        val view: View,
        val rateFormatter: RateFormatter,
        private val interactor: Interactor,
        private val coinCode: String,
        private val currency: Currency,
        private val factory: RateChartViewFactory)
    : ViewModel(), ViewDelegate, InteractorDelegate {

    private var chartType = interactor.defaultChartType ?: ChartType.DAILY
    private var emaIsEnabled = false
    private var macdIsEnabled = false
    private var rsiIsEnabled = false

    private var chartInfo: ChartInfo? = null
        set(value) {
            field = value
            updateChartInfo()
        }

    private var marketInfo: MarketInfo? = null
        set(value) {
            field = value
            updateMarketInfo()
        }

    //  ViewDelegate

    override fun viewDidLoad() {
        view.setChartType(chartType)

        marketInfo = interactor.getMarketInfo(coinCode, currency.code)
        interactor.observeMarketInfo(coinCode, currency.code)

        fetchChartInfo()
    }

    override fun onSelect(type: ChartType) {
        if (chartType == type)
            return

        chartType = type
        interactor.defaultChartType = type

        fetchChartInfo()
    }

    override fun onTouchSelect(point: PointInfo) {
        val price = CurrencyValue(currency, point.value.toBigDecimal())

        if (macdIsEnabled){
            view.showSelectedPointInfo(ChartPointViewItem(point.timestamp, price, null, point.macdInfo))
        } else {
            val volume = point.volume?.let { volume ->
                CurrencyValue(currency, volume.toBigDecimal())
            }
            view.showSelectedPointInfo(ChartPointViewItem(point.timestamp, price, volume, null))
        }
    }

    override fun toggleEma() {
        emaIsEnabled = !emaIsEnabled
        view.setEmaEnabled(emaIsEnabled)
    }

    override fun toggleMacd() {
        if (rsiIsEnabled){
            toggleRsi()
        }

        macdIsEnabled = !macdIsEnabled
        view.setMacdEnabled(macdIsEnabled)
    }

    override fun toggleRsi() {
        if (macdIsEnabled){
            toggleMacd()
        }

        rsiIsEnabled = !rsiIsEnabled
        view.setRsiEnabled(rsiIsEnabled)
    }

    private fun fetchChartInfo() {
        view.showSpinner()

        chartInfo = interactor.getChartInfo(coinCode, currency.code, chartType)
        interactor.observeChartInfo(coinCode, currency.code, chartType)
    }

    private fun updateMarketInfo() {
        val market = marketInfo ?: return

        view.showMarketInfo(factory.createMarketInfo(market, currency, coinCode))

        val info = chartInfo ?: return
        try {
            view.showChartInfo(factory.createChartInfo(chartType, info, market))
        } catch (e: Exception) {
            view.showError(e)
        }
    }

    private fun updateChartInfo() {
        val info = chartInfo ?: return

        view.hideSpinner()

        try {
            view.showChartInfo(factory.createChartInfo(chartType, info, marketInfo))
        } catch (e: Exception) {
            view.showError(e)
        }
    }

    //  InteractorDelegate

    override fun onUpdate(marketInfo: MarketInfo) {
        this.marketInfo = marketInfo
    }

    override fun onUpdate(chartInfo: ChartInfo) {
        this.chartInfo = chartInfo
    }

    override fun onError(ex: Throwable) {
        view.hideSpinner()
        view.showError(ex)
    }

    //  ViewModel

    override fun onCleared() {
        interactor.clear()
    }
}
