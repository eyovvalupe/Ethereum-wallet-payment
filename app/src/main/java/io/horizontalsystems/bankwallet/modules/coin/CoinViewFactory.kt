package io.horizontalsystems.bankwallet.modules.coin

import androidx.annotation.DrawableRes
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartDataFactory
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.chartview.models.MacdInfo
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.*
import io.horizontalsystems.views.ListPosition
import java.lang.Long.max
import java.math.BigDecimal
import java.net.URI

data class ChartInfoData(
    val chartData: ChartData,
    val chartType: ChartView.ChartType,
    val maxValue: String?,
    val minValue: String?
)

data class ChartPointViewItem(
    val date: Long,
    val price: CurrencyValue,
    val volume: CurrencyValue?,
    val macdInfo: MacdInfo?
)

data class MarketTickerViewItem(
    val market: String,
    val marketImageUrl: String?,
    val pair: String,
    val rate: String,
    val volume: String,
) {
    fun areItemsTheSame(other: MarketTickerViewItem): Boolean {
        return market == other.market && pair == other.pair
    }

    fun areContentsTheSame(other: MarketTickerViewItem): Boolean {
        return rate == other.rate && volume == other.volume && marketImageUrl == other.marketImageUrl
    }
}

sealed class RoiViewItem {
    abstract var listPosition: ListPosition?
    class HeaderRowViewItem(
        val title: String,
        val periods: List<TimePeriod>,
        override var listPosition: ListPosition? = null
    ) : RoiViewItem()

    class RowViewItem(
        val title: String,
        val values: List<BigDecimal?>,
        override var listPosition: ListPosition? = null
    ) : RoiViewItem()
}

sealed class ContractInfo(val uid: String, @DrawableRes val logoResId: Int) {
    class Erc20(address: String) : ContractInfo(shortenAddress(address), R.drawable.logo_ethereum_24)
    class Bep20(address: String) : ContractInfo(shortenAddress(address), R.drawable.logo_binancesmartchain_24)
    class Bep2(val symbol: String) : ContractInfo(symbol, R.drawable.logo_bep2_24)

    companion object {
        private fun shortenAddress(address: String) = if (address.length >= 20) {
            address.take(8) + "..." + address.takeLast(8)
        } else {
            address
        }
    }
}

data class CoinDataItem(
    val title: String,
    val value: String? = null,
    val valueLabeled: String? = null,
    val valueLabeledBackground: Int? = null,
    val valueDecorated: Boolean = false,
    @DrawableRes val icon: Int? = null,
    var listPosition: ListPosition? = null,
    val rankLabel: String? = null
)

sealed class InvestorItem {
    data class Header(val title: String) : InvestorItem()
    data class Fund(
        val name: String,
        val url: String,
        val cleanedUrl: String,
        val position: ListPosition
    ) : InvestorItem()
}

sealed class MajorHolderItem {
    object Header : MajorHolderItem()

    class Item(
        val index: Int,
        val address: String,
        val share: BigDecimal,
        val sharePercent: String
    ) : MajorHolderItem()
}

data class CoinLink(
    val url: String,
    val linkType: LinkType,
    val title: String,
    val icon: Int,
    var listPosition: ListPosition? = null
)

data class LastPoint(
    val rate: BigDecimal,
    val timestamp: Long,
    val rateDiff24h: BigDecimal
)

class CoinViewFactory(
    private val currency: Currency,
    private val numberFormatter: IAppNumberFormatter
) {
    fun createChartInfoData(
        type: ChartType,
        chartInfo: ChartInfo,
        lastPoint: LastPoint?
    ): ChartInfoData {
        val chartType = when (type) {
            ChartType.TODAY -> ChartView.ChartType.TODAY
            ChartType.DAILY -> ChartView.ChartType.DAILY
            ChartType.WEEKLY -> ChartView.ChartType.WEEKLY
            ChartType.WEEKLY2 -> ChartView.ChartType.WEEKLY2
            ChartType.MONTHLY -> ChartView.ChartType.MONTHLY
            ChartType.MONTHLY3 -> ChartView.ChartType.MONTHLY3
            ChartType.MONTHLY6 -> ChartView.ChartType.MONTHLY6
            ChartType.MONTHLY12 -> ChartView.ChartType.MONTHLY12
            ChartType.MONTHLY24 -> ChartView.ChartType.MONTHLY24
        }
        val chartData = createChartData(chartInfo, lastPoint, chartType)
        val maxValue = numberFormatter.formatFiat(chartData.valueRange.upper, currency.symbol, 0, 2)
        val minValue = numberFormatter.formatFiat(chartData.valueRange.lower, currency.symbol, 0, 2)

        return ChartInfoData(chartData, chartType, maxValue, minValue)
    }

    fun getRoi(performance: Map<String, Map<TimePeriod, BigDecimal>>): List<RoiViewItem> {
        val rows = mutableListOf<RoiViewItem>()

        val timePeriods = performance.map { it.value.keys }.flatten().distinct()
        rows.add(RoiViewItem.HeaderRowViewItem("ROI", timePeriods, ListPosition.First))
        performance.forEach { (vsCurrency, performanceVsCurrency) ->
            if (performanceVsCurrency.isNotEmpty()) {
                val values = timePeriods.map { performanceVsCurrency[it] }
                rows.add(RoiViewItem.RowViewItem("vs ${vsCurrency.uppercase()}", values, ListPosition.Middle))
            }
        }
        rows.lastOrNull()?.listPosition = ListPosition.Last

        return rows
    }

    fun getMarketData(overview: MarketInfoOverview, currency: Currency, coinCode: String): MarketDataViewItem {
        val marketCapString = overview.marketCap?.let {
            formatFiatShortened(it, currency.symbol)
        }

        val marketCapRankString = overview.marketCapRank?.let { "#$it" }

        val volumeString = overview.volume24h?.let {
            formatFiatShortened(it, currency.symbol)
        }

        val tvlString = overview.tvl?.let {
            formatFiatShortened(it, currency.symbol)
        }

        val dilutedMarketCapString = overview.dilutedMarketCap?.let {
            formatFiatShortened(it, currency.symbol)
        }

        val genesisDateString = overview.genesisDate?.let {
            DateHelper.formatDate(it, "MMM d, yyyy")
        }

        val supplyString = overview.circulatingSupply?.let {
            val (shortenValue, suffix) = numberFormatter.shortenValue(it)
            "$shortenValue $suffix $coinCode"
        }

        val totalSupplyString = overview.totalSupply?.let {
            val (shortenValue, suffix) = numberFormatter.shortenValue(it)
            "$shortenValue $suffix $coinCode"
        }

        return MarketDataViewItem(
            marketCap = marketCapString,
            marketCapRank = marketCapRankString,
            volume24h = volumeString,
            tvl = tvlString,
            genesisDate = genesisDateString,
            circulatingSupply = supplyString,
            totalSupply = totalSupplyString,
            dilutedMarketCap = dilutedMarketCapString
        )
    }

    fun getLinks(coinMarketDetails: MarketInfoOverview, guideUrl: String?): List<CoinLink> {
        val linkTypes = listOf(
            LinkType.Guide,
            LinkType.Website,
            LinkType.Whitepaper,
            LinkType.Reddit,
            LinkType.Twitter,
            LinkType.Telegram,
            LinkType.Github,
        )

        val links = linkTypes.mapNotNull { linkType ->
            if (linkType == LinkType.Guide) {
                guideUrl?.let {
                    CoinLink(guideUrl, linkType, getTitle(linkType, guideUrl), getIcon(linkType))
                }
            } else {
                coinMarketDetails.links[linkType]?.let { link ->
                    val trimmed = link.trim()
                    if (trimmed.isNotBlank()) {
                        CoinLink(trimmed, linkType, getTitle(linkType, trimmed), getIcon(linkType))
                    } else {
                        null
                    }
                }
            }
        }

        links.forEachIndexed { index, link ->
            link.listPosition = ListPosition.getListPosition(links.size, index)
        }

        return links
    }

    fun getFormattedLatestRate(currencyValue: CurrencyValue): String {
        return numberFormatter.formatFiat(currencyValue.value, currencyValue.currency.symbol, 2, 4)
    }

    private fun getIcon(linkType: LinkType): Int {
        return when (linkType) {
            LinkType.Guide -> R.drawable.ic_academy_20
            LinkType.Website -> R.drawable.ic_globe_20
            LinkType.Whitepaper -> R.drawable.ic_clipboard_20
            LinkType.Twitter -> R.drawable.ic_twitter_20
            LinkType.Telegram -> R.drawable.ic_telegram_20
            LinkType.Reddit -> R.drawable.ic_reddit_20
            LinkType.Github -> R.drawable.ic_github_20
        }
    }

    private fun getTitle(linkType: LinkType, link: String? = null): String {
        return when (linkType) {
            LinkType.Guide -> Translator.getString(R.string.CoinPage_Guide)
            LinkType.Website -> {
                link?.let { URI(it).host.replaceFirst("www.", "") }
                    ?: Translator.getString(R.string.CoinPage_Website)
            }
            LinkType.Whitepaper -> Translator.getString(R.string.CoinPage_Whitepaper)
            LinkType.Twitter -> {
                link?.split("/")?.lastOrNull()?.replaceFirst("@", "")?.let { "@$it" }
                    ?: Translator.getString(R.string.CoinPage_Twitter)
            }
            LinkType.Telegram -> Translator.getString(R.string.CoinPage_Telegram)
            LinkType.Reddit -> Translator.getString(R.string.CoinPage_Reddit)
            LinkType.Github -> Translator.getString(R.string.CoinPage_Github)
//            LinkType.YOUTUBE -> Translator.getString(R.string.CoinPage_Youtube)
        }
    }

    private fun createChartData(
        chartInfo: ChartInfo,
        lastPoint: LastPoint?,
        chartType: ChartView.ChartType
    ): ChartData {
        val points = chartInfo.points.map {
            ChartPoint(
                it.value.toFloat(),
                it.volume?.toFloat(),
                it.timestamp
            )
        }.toMutableList()
        val chartInfoLastPoint = chartInfo.points.lastOrNull()
        var endTimestamp = chartInfo.endTimestamp

        if (lastPoint != null && chartInfoLastPoint?.timestamp != null && lastPoint.timestamp > chartInfoLastPoint.timestamp) {
            endTimestamp = max(lastPoint.timestamp, endTimestamp)
            points.add(ChartPoint(lastPoint.rate.toFloat(), null, lastPoint.timestamp))

            if (chartType == ChartView.ChartType.DAILY) {
                val startTimestamp = lastPoint.timestamp - 24 * 60 * 60
                val startValue =
                    lastPoint.rate.multiply(100.toBigDecimal()) / lastPoint.rateDiff24h.add(100.toBigDecimal())
                val startPoint = ChartPoint(startValue.toFloat(), null, startTimestamp)

                points.removeIf { it.timestamp <= startTimestamp }
                points.add(0, startPoint)
            }
        }

        return ChartDataFactory.build(
            points,
            chartInfo.startTimestamp,
            endTimestamp,
            chartInfo.isExpired
        )
    }

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        val shortCapValue = numberFormatter.shortenValue(value)
        return numberFormatter.formatFiat(
            shortCapValue.first,
            symbol,
            0,
            2
        ) + " " + shortCapValue.second
    }

}

data class MarketDataViewItem(
    val marketCap: String?,
    val marketCapRank: String?,
    val volume24h: String?,
    val tvl: String?,
    val genesisDate: String?,
    val circulatingSupply: String?,
    val totalSupply: String?,
    val dilutedMarketCap: String?,
)
