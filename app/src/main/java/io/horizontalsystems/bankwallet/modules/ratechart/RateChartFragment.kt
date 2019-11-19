package io.horizontalsystems.bankwallet.modules.ratechart

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.lib.chartview.ChartView
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import io.horizontalsystems.xrateskit.entities.ChartType
import kotlinx.android.synthetic.main.view_bottom_sheet_chart.*
import java.math.BigDecimal
import java.util.*

class RateChartFragment(private val coin: Coin) : BaseBottomSheetDialogFragment(), ChartView.Listener {

    private lateinit var presenter: RateChartPresenter
    private lateinit var presenterView: RateChartView

    private val formatter = App.numberFormatter
    private var actions = mapOf<ChartType, View>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.view_bottom_sheet_chart)

        setTitle(getString(R.string.Charts_Title, coin.title))
        setHeaderIcon(LayoutHelper.getCoinDrawableResource(coin.code))

        chartView.listener = this
        chartView.setIndicator(chartViewIndicator)

        presenter = ViewModelProviders.of(this, RateChartModule.Factory(coin)).get(RateChartPresenter::class.java)
        presenterView = presenter.view as RateChartView

        observeData()
        bindActions()
    }

    override fun onShow() {
        presenter.viewDidLoad()
    }

    private fun observeData() {
        presenterView.showSpinner.observe(viewLifecycleOwner, Observer {
            setViewVisibility(chartView, chartError, isVisible = false)
            setViewVisibility(chartViewSpinner, isVisible = true)
        })

        presenterView.hideSpinner.observe(viewLifecycleOwner, Observer {
            setViewVisibility(chartView, isVisible = true)
            setViewVisibility(chartViewSpinner, isVisible = false)
        })

        presenterView.setDefaultMode.observe(viewLifecycleOwner, Observer { type ->
            actions[type]?.let { it.isActivated = true }
        })

        presenterView.showChartInfo.observe(viewLifecycleOwner, Observer { item ->
            chartView.visibility = View.VISIBLE
            chartView.setData(item.chartPoints, item.chartType, item.startTimestamp, item.endTimestamp)

            context?.let { coinRateDiff.bind(item.diffValue, it, true) }

            coinRateHighTitle.text = getString(R.string.Charts_Rate_High, actionTitle(item.chartType))
            coinRateHigh.text = formatter.format(item.highValue, canUseLessSymbol = false, trimmable = true)

            coinRateLowTitle.text = getString(R.string.Charts_Rate_Low, actionTitle(item.chartType))
            coinRateLow.text = formatter.format(item.lowValue, canUseLessSymbol = false, trimmable = true)
            setViewVisibility(highLowWrap, isVisible = true)
        })

        presenterView.showMarketInfo.observe(viewLifecycleOwner, Observer { item ->
            setSubtitle(DateHelper.getFullDateWithShortMonth(item.timestamp * 1000))

            coinRateLast.text = formatter.format(item.rateValue, canUseLessSymbol = false)

            val shortCapValue = shortenValue(item.marketCap.value)
            val marketCap = CurrencyValue(item.marketCap.currency, shortCapValue.first)
            coinMarketCap.text = formatter.format(marketCap, canUseLessSymbol = false) + shortCapValue.second

            val shortVolumeValue = shortenValue(item.volume.value)
            val volume = CurrencyValue(item.volume.currency, shortVolumeValue.first)
            volumeValue.text = formatter.format(volume, canUseLessSymbol = false) + shortVolumeValue.second

            circulationValue.text = formatter.format(item.supply, trimmable = true)

            totalSupplyValue.text = item.maxSupply?.let {
                formatter.format(it, trimmable = true)
            } ?: run {
                getString(R.string.NotAvailable)
            }

            setViewVisibility(highLowWrap, isVisible = true)
        })

        presenterView.setSelectedPoint.observe(viewLifecycleOwner, Observer { (time, value, type) ->
            val outputFormat = when (type) {
                ChartType.DAILY,
                ChartType.WEEKLY -> "MMM d, yyyy 'at' HH:mm a"
                else -> "MMM d, yyyy"
            }
            pointInfoPrice.text = formatter.format(value, canUseLessSymbol = false)
            pointInfoDate.text = DateHelper.formatDate(Date(time * 1000), outputFormat)
        })

        presenterView.showError.observe(viewLifecycleOwner, Observer {
            chartView.visibility = View.INVISIBLE
            chartError.visibility = View.VISIBLE
            chartError.text = getString(R.string.Charts_Error_NotAvailable)
        })
    }

    //  ChartView Listener

    override fun onTouchDown() {
        shouldCloseOnSwipe = false

        setViewVisibility(chartPointsInfo, chartViewIndicator, isVisible = true)
        setViewVisibility(chartActions, isVisible = false)
    }

    override fun onTouchUp() {
        shouldCloseOnSwipe = true

        setViewVisibility(chartPointsInfo, chartViewIndicator, isVisible = false)
        setViewVisibility(chartActions, isVisible = true)
    }

    override fun onTouchSelect(point: ChartPoint) {
        presenter.onTouchSelect(point)
    }

    private fun bindActions() {
        actions = mapOf(
                Pair(ChartType.DAILY, button1D),
                Pair(ChartType.WEEKLY, button1W),
                Pair(ChartType.MONTHLY, button1M),
                Pair(ChartType.MONTHLY6, button6M),
                Pair(ChartType.MONTHLY12, button1Y)
        )

        actions.forEach { (type, action) ->
            action.setOnClickListener {
                presenter.onSelect(type)
                resetActions(it)
            }
        }
    }

    private fun actionTitle(chartType: ChartView.ChartType): String {
        return when (chartType) {
            ChartView.ChartType.DAILY -> getString(R.string.Charts_TimeDuration_Day)
            ChartView.ChartType.WEEKLY -> getString(R.string.Charts_TimeDuration_Week)
            ChartView.ChartType.MONTHLY -> getString(R.string.Charts_TimeDuration_Month)
            ChartView.ChartType.MONTHLY6 -> getString(R.string.Charts_TimeDuration_HalfYear)
            ChartView.ChartType.MONTHLY18 -> getString(R.string.Charts_TimeDuration_Year)
        }
    }

    private fun resetActions(current: View) {
        actions.values.forEach { it.isActivated = false }
        setViewVisibility(highLowWrap, isVisible = false)
        current.isActivated = true
    }

    private fun setViewVisibility(vararg views: View, isVisible: Boolean) {
        views.forEach {
            if (isVisible)
                it.visibility = View.VISIBLE else
                it.visibility = View.INVISIBLE
        }
    }

    // Need to move this to helpers
    private fun shortenValue(number: Number): Pair<BigDecimal, String> {
        val suffix = arrayOf(
                " ",
                getString(R.string.Charts_MarketCap_Thousand),
                getString(R.string.Charts_MarketCap_Million),
                getString(R.string.Charts_MarketCap_Billion),
                getString(R.string.Charts_MarketCap_Trillion)) // "P", "E"

        val valueLong = number.toLong()
        val value = Math.floor(Math.log10(valueLong.toDouble())).toInt()
        val base = value / 3

        var returnSuffix = ""
        var valueDecimal = valueLong.toBigDecimal()
        if (value >= 3 && base < suffix.size) {
            valueDecimal = (valueLong / Math.pow(10.0, (base * 3).toDouble())).toBigDecimal()
            returnSuffix = suffix[base]
        }

        return Pair(valueDecimal, returnSuffix)
    }
}
