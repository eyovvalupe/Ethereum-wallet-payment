package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartView
import io.horizontalsystems.bankwallet.modules.market.DiffValue
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.TabBalance
import io.horizontalsystems.core.entities.Currency

@Composable
fun ChartInfo(
    chartInfo: CoinChartAdapter.ViewItemWrapper,
    currency: Currency,
    chartViewType: CoinChartAdapter.ChartViewType,
    listener: CoinChartAdapter.Listener
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(), // Occupy the max size in the Compose UI tree
        factory = { context ->
            CoinChartView(context).apply {
                setCurrency(currency)
                setChartViewType(chartViewType)
                setListener(listener)
                bindNew(chartInfo)
            }
        },
        update = { view ->
            view.bindNew(chartInfo)
        }
    )
}

@Immutable
data class ChartInfoHeaderItem(
    val value: String?,
    val diff: DiffValue?
)

@Composable
fun ChartInfoHeader(item: ChartInfoHeaderItem) {
    TabBalance {
        Text(
            modifier = Modifier.padding(end = 8.dp),
            text = item.value ?: "",
            style = ComposeAppTheme.typography.headline1,
            color = ComposeAppTheme.colors.leah
        )

        item.diff?.let { diff ->
            val color = when (diff) {
                is DiffValue.Positive,
                is DiffValue.NoDiff -> ComposeAppTheme.colors.remus
                is DiffValue.Negative -> ComposeAppTheme.colors.lucian
            }
            Text(
                text = diff.value,
                style = ComposeAppTheme.typography.subhead1,
                color = color
            )
        }
    }
}
