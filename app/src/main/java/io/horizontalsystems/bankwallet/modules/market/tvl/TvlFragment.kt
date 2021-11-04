package io.horizontalsystems.bankwallet.modules.market.tvl

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.ChartInfo
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.ChartInfoHeader
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule.SelectorDialogState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.findNavController

class TvlFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val viewModel by viewModels<TvlViewModel> { TvlModule.Factory() }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    TvlScreen(viewModel) { onCoinClick(it) }
                }
            }
        }
    }

    private fun onCoinClick(coinUid: String) {
        val arguments = CoinFragment.prepareParams(coinUid)

        findNavController().navigate(R.id.coinFragment, arguments, navOptions())
    }

    @Composable
    private fun TvlScreen(
        viewModel: TvlViewModel,
        onCoinClick: (String) -> Unit
    ) {
        val viewState by viewModel.viewStateLiveData.observeAsState()
        val chartData by viewModel.chartLiveData.observeAsState()
        val tvlData by viewModel.tvlLiveData.observeAsState()
        val loading by viewModel.loadingLiveData.observeAsState(false)
        val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
        val chainSelectorDialogState by viewModel.chainSelectorDialogStateLiveData.observeAsState(SelectorDialogState.Closed)

        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.MarketGlobalMetrics_TvlInDefi),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = {
                            findNavController().popBackStack()
                        }
                    )
                )
            )

            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing || loading),
                onRefresh = {
                    viewModel.refresh()
                },
                indicator = { state, trigger ->
                    SwipeRefreshIndicator(
                        state = state,
                        refreshTriggerDistance = trigger,
                        scale = true,
                        backgroundColor = ComposeAppTheme.colors.claude,
                        contentColor = ComposeAppTheme.colors.oz,
                    )
                }
            ) {
                LazyColumn {
                    when (viewState) {
                        ViewState.Error -> {
                            item {
                                ListErrorView(
                                    stringResource(R.string.Market_SyncError)
                                ) {
                                    viewModel.onErrorClick()
                                }
                            }
                        }
                        ViewState.Success -> {
                            chartData?.let { chartData ->
                                item {
                                    ChartInfoHeader(chartData.subtitle)

                                    ChartInfo(
                                        CoinChartAdapter.ViewItemWrapper(chartData.chartInfoData),
                                        chartData.currency,
                                        CoinChartAdapter.ChartViewType.MarketMetricChart,
                                        object : CoinChartAdapter.Listener {
                                            override fun onChartTouchDown() = Unit

                                            override fun onChartTouchUp() = Unit

                                            override fun onTabSelect(chartType: ChartView.ChartType) = Unit
                                        })
                                }
                            }

                            tvlData?.let { tvlData ->
                                item {
                                    TvlMenu(
                                        tvlData.menu,
                                        viewModel::onClickChainSelector,
                                        viewModel::onToggleSortType,
                                        viewModel::onToggleTvlDiffType
                                    )
                                }

                                items(tvlData.coinTvlViewItems) { item ->
                                    MarketCoin(
                                        item.fullCoin.coin.name,
                                        item.fullCoin.coin.code,
                                        item.fullCoin.coin.iconUrl,
                                        item.fullCoin.iconPlaceholder,
                                        item.tvl,
                                        MarketDataValue.DiffNew(item.tvlDiff),
                                        item.rank
                                    ) { onCoinClick.invoke(item.fullCoin.coin.uid) }
                                }
                            }
                        }
                    }
                }
                // chain selector dialog
                when (val option = chainSelectorDialogState) {
                    is SelectorDialogState.Opened -> {
                        AlertGroup(
                            R.string.MarketGlobalMetrics_ChainSelectorTitle,
                            option.select,
                            viewModel::onSelectChain,
                            viewModel::onChainSelectorDialogDismiss
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TvlMenu(
        menu: TvlModule.Menu,
        onClickChainSelector: () -> Unit,
        onToggleSortType: () -> Unit,
        onToggleTvlDiffType: () -> Unit
    ) {
        Header(borderBottom = true) {
            Box(modifier = Modifier.weight(1f)) {
                SortMenu(menu.chainSelect.selected.title) {
                    onClickChainSelector()
                }
            }
            ButtonSecondaryCircle(
                modifier = Modifier.padding(end = 16.dp),
                icon = if (menu.sortDescending) R.drawable.ic_arrow_down_20 else R.drawable.ic_arrow_up_20,
                onClick = { onToggleSortType() }
            )
            ButtonSecondaryCircle(
                modifier = Modifier.padding(end = 16.dp),
                icon = if (menu.tvlDiffType == TvlModule.TvlDiffType.Percent) R.drawable.ic_percent_20 else R.drawable.ic_usd_20,
                onClick = { onToggleTvlDiffType() }
            )
        }
    }

    @Composable
    private fun MarketCoin(
        coinName: String,
        coinCode: String,
        coinIconUrl: String,
        coinIconPlaceholder: Int,
        tvl: String,
        marketDataValue: MarketDataValue? = null,
        label: String? = null,
        onClick: (() -> Unit)? = null
    ) {
        MultilineClear(
            onClick = onClick,
            borderBottom = true
        ) {
            CoinImage(
                iconUrl = coinIconUrl,
                placeholder = coinIconPlaceholder,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                MarketCoinFirstRow(coinName, tvl)
                Spacer(modifier = Modifier.height(3.dp))
                MarketCoinSecondRow(coinCode, marketDataValue, label)
            }
        }
    }

}
