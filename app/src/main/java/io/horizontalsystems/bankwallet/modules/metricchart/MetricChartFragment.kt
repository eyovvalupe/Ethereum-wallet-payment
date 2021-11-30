package io.horizontalsystems.bankwallet.modules.metricchart

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.ChartInfo
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.ChartInfoHeader
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import io.horizontalsystems.chartview.ChartView
import kotlinx.android.synthetic.main.fragment_market_global.*

class MetricChartFragment : BaseBottomSheetDialogFragment() {

    private val coinUid by lazy {
        requireArguments().getString(coinUidKey) ?: ""
    }
    private val metricChartType by lazy {
        requireArguments().getParcelable(metricChartTypeKey) ?: MetricChartModule.MetricChartType.TradingVolume
    }

    private val title by lazy {
        requireArguments().getString(titleKey) ?: ""
    }

    private val viewModel by viewModels<MetricChartViewModel> { MetricChartModule.Factory(coinUid, metricChartType) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setContentView(R.layout.fragment_market_global)

        setTitle(getString(viewModel.title))
        setSubtitle(getString(R.string.MarketGlobalMetrics_Chart))
        setHeaderIcon(R.drawable.ic_chart_24)

        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        composeView.setContent {
            TradingVolumeChartScreen(viewModel, title)
        }
    }

    @Composable
    private fun TradingVolumeChartScreen(viewModel: MetricChartViewModel, coinName: String) {
        val chartData by viewModel.chartLiveData.observeAsState()
        val chartTypes by viewModel.chartTypes.observeAsState(listOf())

        ComposeAppTheme {
            Column {
                chartData?.let { chartData ->
                    ChartInfoHeader(chartData.subtitle)

                    ChartInfo(
                        CoinChartAdapter.ViewItemWrapper(chartData.chartInfoData),
                        chartData.currency,
                        CoinChartAdapter.ChartViewType.MarketMetricChart,
                        chartTypes,
                        object : CoinChartAdapter.Listener {
                            override fun onChartTouchDown() = Unit

                            override fun onChartTouchUp() = Unit

                            override fun onTabSelect(chartType: ChartView.ChartType) {
                                viewModel.onSelectChartType(chartType)
                            }
                        })
                }

                BottomSheetText(
                    text = stringResource(id = R.string.MarketGlobalMetrics_VolumeDescriptionCoin, coinName)
                )
                BottomSheetText(
                    text = stringResource(id = R.string.Market_PoweredByApi)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    @Composable
    fun BottomSheetText(text: String) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 24.dp),
            text = text,
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead2
        )
    }

    companion object {
        private const val coinUidKey = "coinUidKey"
        private const val metricChartTypeKey = "metricChartTypeKey"
        private const val titleKey = "titleKey"

        fun show(fragmentManager: FragmentManager, coinUid: String, title: String, type: MetricChartModule.MetricChartType) {
            val fragment = MetricChartFragment()
            fragment.arguments = bundleOf(coinUidKey to coinUid, titleKey to title, metricChartTypeKey to type)
            fragment.show(fragmentManager, "metric_chart_dialog")
        }
    }
}
