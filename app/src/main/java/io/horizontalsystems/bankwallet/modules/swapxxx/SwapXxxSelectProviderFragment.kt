package io.horizontalsystems.bankwallet.modules.swapxxx

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_green50
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah

class SwapXxxSelectProviderFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapXxxSelectProviderScreen(navController)
    }
}

@Composable
fun SwapXxxSelectProviderScreen(navController: NavController) {
    val viewModel = viewModel<SwapXxxViewModel>(
        viewModelStoreOwner = navController.previousBackStackEntry!!,
        factory = SwapXxxViewModel.Factory()
    )

    val uiState = viewModel.uiState

    SwapXxxSelectProviderScreenInner(
        onClickClose = navController::popBackStack,
        quotes = uiState.quotes,
        bestQuote = uiState.bestQuote,
        selectedQuote = uiState.selectedQuote,
    ) {
        viewModel.onSelectQuote(it)
        navController.popBackStack()
    }
}

@Composable
private fun SwapXxxSelectProviderScreenInner(
    onClickClose: () -> Unit,
    quotes: List<SwapProviderQuote>,
    bestQuote: SwapProviderQuote?,
    selectedQuote: SwapProviderQuote?,
    onSelectQuote: (SwapProviderQuote) -> Unit
) {
    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.Swap),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Done),
                        onClick = onClickClose
                    )
                ),
            )
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
    ) {
        LazyColumn(
            modifier = Modifier.padding(it),
            contentPadding = PaddingValues(top = 12.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quotes) { quote ->
                val borderColor = if (quote == selectedQuote) {
                    ComposeAppTheme.colors.yellow50
                } else {
                    ComposeAppTheme.colors.steel20
                }
                RowUniversal(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp),
                    onClick = { onSelectQuote.invoke(quote) }
                ) {
                    val provider = quote.provider
                    Image(
                        modifier = Modifier.size(32.dp),
                        painter = painterResource(provider.icon),
                        contentDescription = null
                    )
                    HSpacer(width = 16.dp)
                    Column {
                        subhead2_leah(
                            text = provider.title,
                            textAlign = TextAlign.End
                        )
                        if (bestQuote == quote) {
                            VSpacer(height = 1.dp)
                            subhead2_green50(
                                text = stringResource(R.string.Swap_BestPrice),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                    HFillSpacer(minWidth = 8.dp)
                    Column(horizontalAlignment = Alignment.End) {
                        subhead2_leah(text = quote.quote.amountOut.toPlainString())
                        VSpacer(height = 1.dp)
                        subhead2_grey(text = "Fee")
                    }
                }
            }
        }
    }
}
