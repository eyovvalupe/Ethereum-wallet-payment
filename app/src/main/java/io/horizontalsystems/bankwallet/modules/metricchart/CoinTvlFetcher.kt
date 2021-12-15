package io.horizontalsystems.bankwallet.modules.metricchart

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.UnsupportedException
import io.horizontalsystems.bankwallet.modules.chart.IChartRepo
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.chartview.ChartView.ChartType
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.TimePeriod
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject

class CoinTvlFetcher(
    private val marketKit: MarketKit,
    private val coinUid: String,
) : IMetricChartFetcher, IChartRepo {

    override val title: Int = R.string.CoinPage_Tvl
    override val description = TranslatableString.ResString(R.string.CoinPage_TvlDescription)
    override val poweredBy = TranslatableString.ResString(R.string.Market_PoweredByDefiLlamaApi)

    override val initialChartType = ChartType.MONTHLY
    override val chartTypes = listOf(
        ChartType.DAILY,
        ChartType.WEEKLY,
        ChartType.MONTHLY,
    )
    override val dataUpdatedObservable = BehaviorSubject.create<Unit>()

    override fun getItems(chartType: ChartType, currency: Currency) = try {
        val timePeriod = getTimePeriod(chartType)
        marketKit.marketInfoTvlSingle(coinUid, currency.code, timePeriod)
            .map { info ->
                info.map { point ->
                    MetricChartModule.Item(point.value, null, point.timestamp)
                }
            }
    } catch (e: Exception) {
        Single.error(e)
    }

    private fun getTimePeriod(chartType: ChartType) = when (chartType) {
        ChartType.DAILY -> TimePeriod.Hour24
        ChartType.WEEKLY -> TimePeriod.Day7
        ChartType.MONTHLY -> TimePeriod.Day30
        else -> throw UnsupportedException("Unsupported chartType $chartType")
    }
}
