package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.MarketInfo
import io.horizontalsystems.xrateskit.entities.TopMarket
import java.math.BigDecimal

object RateListModule {

    interface IView {
        fun setDate(lastUpdateTime: Long)
        fun setViewItems(viewItems: List<ViewItem>)
    }

    interface IRouter {
        fun openChart(coinCode: String, coinTitle: String)
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onCoinClicked(coinViewItem: ViewItem.CoinViewItem)
    }

    interface IInteractor {
        val currency: Currency
        val coins: List<Coin>

        fun clear()
        fun getMarketInfo(coinCode: String, currencyCode: String): MarketInfo?
        fun subscribeToMarketInfo(currencyCode: String)
        fun setupXRateManager(coinCodes: List<String>)
        fun getTopList()
    }

    interface IInteractorDelegate {
        fun didUpdateMarketInfo(marketInfos: Map<String, MarketInfo>)
        fun didFetchedTopMarketList(items: List<TopMarket>)
        fun didFailToFetchTopList()
    }

    interface IRateListFactory {
        fun portfolioViewItems(coins: List<Coin>, currency: Currency, marketInfos: Map<String, MarketInfo>): List<ViewItem.CoinViewItem>
        fun topListViewItems(topMarketList: List<TopMarket>, currency: Currency): List<ViewItem.CoinViewItem>
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = RateListView()
            val router = RateListRouter()
            val interactor = RatesInteractor(
                    App.xRateManager,
                    App.currencyManager,
                    App.walletStorage,
                    App.appConfigProvider,
                    RateListSorter())
            val presenter = RateListPresenter(view, router, interactor, RateListFactory(App.numberFormatter))

            interactor.delegate = presenter

            return presenter as T
        }
    }

}

class RateListSorter {
    fun smartSort(coins: List<Coin>, featuredCoins: List<Coin>): List<Coin> {
        return if (coins.isEmpty()) {
            featuredCoins
        } else {
            val filteredByPredefined = featuredCoins.filter { coins.contains(it) }
            val remainingCoins = coins.filter { !featuredCoins.contains(it) }.sortedBy { it.code }
            val mergedList = mutableListOf<Coin>()
            mergedList.addAll(filteredByPredefined)
            mergedList.addAll(remainingCoins)
            mergedList
        }
    }
}

data class CoinItem(val coinCode: String, val coinName: String, var rate: String?, var diff: BigDecimal?, var coin: Coin? = null, var timestamp: Long, var rateDimmed: Boolean)

sealed class ViewItem{
    object PortfolioHeader: ViewItem()
    object TopListHeader: ViewItem()
    object LoadingSpinner: ViewItem()
    object SourceText: ViewItem()
    data class CoinViewItem(val coinItem: CoinItem, val last: Boolean): ViewItem()
}
