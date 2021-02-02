package io.horizontalsystems.bankwallet.modules.market.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.MarketViewModel
import io.horizontalsystems.bankwallet.modules.market.discovery.PoweredByAdapter
import io.horizontalsystems.bankwallet.modules.market.metrics.MarketMetricsAdapter
import io.horizontalsystems.bankwallet.modules.market.metrics.MarketMetricsModule
import io.horizontalsystems.bankwallet.modules.market.metrics.MarketMetricsViewModel
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartFragment
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.navGraphViewModels
import kotlinx.android.synthetic.main.fragment_overview.*

class MarketOverviewFragment : BaseFragment(), ViewHolderMarketOverviewItem.Listener {

    private val marketMetricsViewModel by viewModels<MarketMetricsViewModel> { MarketMetricsModule.Factory() }
    private val marketOverviewViewModel by viewModels<MarketOverviewViewModel> { MarketOverviewModule.Factory() }
    private val marketViewModel by navGraphViewModels<MarketViewModel>(R.id.mainFragment)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val marketMetricsAdapter = MarketMetricsAdapter(marketMetricsViewModel, viewLifecycleOwner)
        val topGainersHeaderAdapter = MarketOverviewSectionHeaderAdapter(
                marketOverviewViewModel.topGainersViewItemsLiveData,
                viewLifecycleOwner,
                MarketOverviewSectionHeaderAdapter.SectionHeaderItem(R.string.RateList_TopWinners, R.drawable.ic_circle_up_20, getString(R.string.Market_SeeAll)) {
                    marketViewModel.onClickSeeAll(MarketModule.ListType.TopGainers)
                }
        )
        val topGainersAdapter = MarketOverviewItemsAdapter(this, marketOverviewViewModel.topGainersViewItemsLiveData, viewLifecycleOwner)

        val topLosersHeaderAdapter = MarketOverviewSectionHeaderAdapter(
                marketOverviewViewModel.topLosersViewItemsLiveData,
                viewLifecycleOwner,
                MarketOverviewSectionHeaderAdapter.SectionHeaderItem(R.string.RateList_TopLosers, R.drawable.ic_circle_down_20, getString(R.string.Market_SeeAll)) {
                    marketViewModel.onClickSeeAll(MarketModule.ListType.TopLosers)
                }
        )
        val topLosersAdapter = MarketOverviewItemsAdapter(this, marketOverviewViewModel.topLosersViewItemsLiveData, viewLifecycleOwner)

        val topByVolumeHeaderAdapter = MarketOverviewSectionHeaderAdapter(
                marketOverviewViewModel.topByVolumeViewItemsLiveData,
                viewLifecycleOwner,
                MarketOverviewSectionHeaderAdapter.SectionHeaderItem(R.string.RateList_TopByVolume, R.drawable.ic_chart_20, getString(R.string.Market_SeeAll)) {
                    marketViewModel.onClickSeeAll(MarketModule.ListType.TopByVolume)
                }
        )
        val topByVolumeAdapter = MarketOverviewItemsAdapter(this, marketOverviewViewModel.topByVolumeViewItemsLiveData, viewLifecycleOwner)

        val poweredByAdapter = PoweredByAdapter(marketOverviewViewModel.showPoweredByLiveData, viewLifecycleOwner)

        coinRatesRecyclerView.adapter = ConcatAdapter(
                marketMetricsAdapter,
                topGainersHeaderAdapter,
                topGainersAdapter,
                topLosersHeaderAdapter,
                topLosersAdapter,
                topByVolumeHeaderAdapter,
                topByVolumeAdapter,
                poweredByAdapter
        )
        coinRatesRecyclerView.itemAnimator = null

        pullToRefresh.setOnRefreshListener {
            marketMetricsAdapter.refresh()
            marketOverviewViewModel.refresh()

            pullToRefresh.isRefreshing = false
        }
    }

    override fun onItemClick(marketViewItem: MarketViewItem) {
        val arguments = RateChartFragment.prepareParams(marketViewItem.coinCode, marketViewItem.coinName, null)

        findNavController().navigate(R.id.rateChartFragment, arguments, navOptions())
    }
}
