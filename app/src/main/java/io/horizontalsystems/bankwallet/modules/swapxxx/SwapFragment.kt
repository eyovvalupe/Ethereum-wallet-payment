package io.horizontalsystems.bankwallet.modules.swapxxx

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromBottomForResult
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.getPriceImpactColor
import io.horizontalsystems.bankwallet.modules.swapxxx.providers.ISwapXxxProvider
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellowWithSpinner
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HSRow
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey50
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_lucian
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class SwapFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapScreen(navController)
    }
}

@Composable
fun SwapScreen(navController: NavController) {
    val currentBackStackEntry = remember { navController.currentBackStackEntry }
    val viewModel = viewModel<SwapViewModel>(
        viewModelStoreOwner = currentBackStackEntry!!,
        factory = SwapViewModel.Factory()
    )
    val uiState = viewModel.uiState

    val selectToken = { onResult: (Token) -> Unit ->
        navController.slideFromBottomForResult(R.id.swapSelectCoinFragment, onResult = onResult)
    }

    SwapScreenInner(
        uiState = uiState,
        onClickClose = navController::popBackStack,
        onClickCoinFrom = {
            selectToken {
                viewModel.onSelectTokenIn(it)
            }
        },
        onClickCoinTo = {
            selectToken {
                viewModel.onSelectTokenOut(it)
            }
        },
        onSwitchPairs = viewModel::onSwitchPairs,
        onEnterAmount = viewModel::onEnterAmount,
        onClickProvider = {
            navController.slideFromBottom(R.id.swapSelectProvider)
        },
        onClickProviderSettings = {
            navController.slideFromRight(R.id.swapSettings)
        },
        onClickTransactionSettings = {
            navController.slideFromRight(R.id.swapTransactionSettings)
        },
        onClickNext = {
            navController.slideFromRight(R.id.swapConfirm)
        }
    )
}

@Composable
private fun SwapScreenInner(
    uiState: SwapUiState,
    onClickClose: () -> Unit,
    onClickCoinFrom: () -> Unit,
    onClickCoinTo: () -> Unit,
    onSwitchPairs: () -> Unit,
    onEnterAmount: (BigDecimal?) -> Unit,
    onClickProvider: () -> Unit,
    onClickTransactionSettings: () -> Unit,
    onClickProviderSettings: () -> Unit,
    onClickNext: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.Swap),
                navigationIcon = {
                    HsBackButton(onClick = onClickClose)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Settings_Title),
                        icon = R.drawable.ic_manage_2_24,
                        onClick = onClickTransactionSettings
                    )
                ),
            )
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            VSpacer(height = 12.dp)
            SwapInput(
                amountIn = uiState.amountIn,
                fiatAmountIn = uiState.fiatAmountIn,
                onSwitchPairs = onSwitchPairs,
                amountOut = uiState.quote?.amountOut,
                fiatAmountOut = uiState.fiatAmountOut,
                onValueChange = onEnterAmount,
                onClickCoinFrom = onClickCoinFrom,
                onClickCoinTo = onClickCoinTo,
                tokenIn = uiState.tokenIn,
                tokenOut = uiState.tokenOut,
                currency = uiState.currency
            )

            VSpacer(height = 12.dp)

            if (uiState.quoting) {
                ButtonPrimaryYellowWithSpinner(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    title = stringResource(R.string.Alert_Loading),
                    enabled = false,
                    onClick = { /*TODO*/ }
                )
            } else {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    title = stringResource(R.string.Swap_Proceed),
                    enabled = uiState.swapEnabled,
                    onClick = onClickNext
                )
            }

            VSpacer(height = 12.dp)

            uiState.error?.let { error ->
                VSpacer(height = 12.dp)
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp)),
                ) {
                    QuoteInfoRow(
                        title = {
                            val errorText = if (error is SwapRouteNotFound) {
                                stringResource(id = R.string.Swap_SwapRouteNotFound)
                            } else {
                                error.javaClass.simpleName
                            }
                            subhead2_lucian(text = errorText)
                        },
                        value = {
                        }
                    )
                }
            }

            uiState.quote?.let { quote ->
                VSpacer(height = 12.dp)
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp))
                        .padding(vertical = 2.dp),
                ) {
                    ProviderField(quote.provider, onClickProvider, onClickProviderSettings)
                    AvailableBalanceField(uiState.tokenIn, uiState.availableBalance)
                    PriceField(quote.tokenIn, quote.tokenOut, quote.amountIn, quote.amountOut)
                    quote.fields.forEach {
                        it.GetContent()
                    }
                    PriceImpactField(uiState.priceImpact, uiState.priceImpactLevel)
                }
            }

            VSpacer(height = 32.dp)
        }
    }
}

@Composable
private fun AvailableBalanceField(tokenIn: Token?, availableBalance: BigDecimal?) {
    if (tokenIn != null && availableBalance != null) {
        QuoteInfoRow(
            title = {
                subhead2_grey(text = stringResource(R.string.Swap_AvailableBalance))
            },
            value = {
                subhead2_leah(text = CoinValue(tokenIn, availableBalance).getFormattedFull())
            }
        )
    }
}

@Composable
private fun PriceImpactField(
    priceImpact: BigDecimal?,
    priceImpactLevel: SwapMainModule.PriceImpactLevel?
) {
    if (priceImpact == null || priceImpactLevel == null) return

    val infoTitle = stringResource(id = R.string.SwapInfo_PriceImpactTitle)
    val infoText = stringResource(id = R.string.SwapInfo_PriceImpactDescription)

    QuoteInfoRow(
        title = {
            subhead2_grey(text = stringResource(R.string.Swap_PriceImpact))

            Image(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable(
                        onClick = {
//                            navController.slideFromBottom(
//                                R.id.feeSettingsInfoDialog,
//                                FeeSettingsInfoDialog.Input(infoTitle, infoText)
//                            )
                        },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    )
                ,
                painter = painterResource(id = R.drawable.ic_info_20),
                contentDescription = ""
            )
        },
        value = {
            Text(
                text = stringResource(R.string.Swap_Percent, priceImpact * BigDecimal.valueOf(-1)),
                style = ComposeAppTheme.typography.subhead2,
                color = getPriceImpactColor(priceImpactLevel),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
private fun ProviderField(
    swapProvider: ISwapXxxProvider,
    onClickProvider: () -> Unit,
    onClickProviderSettings: () -> Unit,
) {
    HSRow(
        modifier = Modifier
            .height(40.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        borderBottom = true,
    ) {
        Selector(
            icon = {
                Image(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(swapProvider.icon),
                    contentDescription = null
                )
            },
            text = {
                subhead1_leah(text = swapProvider.title)
            },
            onClickSelect = onClickProvider
        )
        HFillSpacer(minWidth = 8.dp)
        Icon(
            modifier = Modifier.clickable(
                onClick = onClickProviderSettings
            ),
            painter = painterResource(R.drawable.ic_manage_2),
            contentDescription = "",
            tint = ComposeAppTheme.colors.grey
        )
    }
}

@Composable
private fun PriceField(tokenIn: Token, tokenOut: Token, amountIn: BigDecimal, amountOut: BigDecimal) {
    var showRegularPrice by remember { mutableStateOf(true) }
    val swapPriceUIHelper = SwapPriceUIHelper(tokenIn, tokenOut, amountIn, amountOut)

    QuoteInfoRow(
        title = {
            subhead2_grey(text = stringResource(R.string.Swap_Price))
        },
        value = {
            subhead2_leah(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            showRegularPrice = !showRegularPrice
                        }
                    ),
                text = if (showRegularPrice) swapPriceUIHelper.priceStr else swapPriceUIHelper.priceInvStr
            )
            HSpacer(width = 8.dp)
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_swap3_20),
                contentDescription = "invert price",
                tint = ComposeAppTheme.colors.grey
            )
        }
    )
}

@Composable
fun QuoteInfoRow(
    title: @Composable() (RowScope.() -> Unit),
    value: @Composable() (RowScope.() -> Unit),
) {
    CellUniversal(borderTop = false) {
        title.invoke(this)
        HFillSpacer(minWidth = 8.dp)
        value.invoke(this)
    }
}

@Composable
private fun SwapInput(
    amountIn: BigDecimal?,
    fiatAmountIn: BigDecimal?,
    onSwitchPairs: () -> Unit,
    amountOut: BigDecimal?,
    fiatAmountOut: BigDecimal?,
    onValueChange: (BigDecimal?) -> Unit,
    onClickCoinFrom: () -> Unit,
    onClickCoinTo: () -> Unit,
    tokenIn: Token?,
    tokenOut: Token?,
    currency: Currency,
) {
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(ComposeAppTheme.colors.lawrence)
                .padding()
        ) {
            SwapCoinInput(
                coinAmount = amountIn,
                fiatAmount = fiatAmountIn,
                currency = currency,
                onValueChange = onValueChange,
                token = tokenIn,
                onClickCoin = onClickCoinFrom
            )
            SwapCoinInput(
                coinAmount = amountOut,
                fiatAmount = fiatAmountOut,
                currency = currency,
                onValueChange = { },
                enabled = false,
                token = tokenOut,
                onClickCoin = onClickCoinTo
            )
        }
        Divider(
            modifier = Modifier.align(Alignment.Center),
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10
        )
        ButtonSecondaryCircle(
            modifier = Modifier.align(Alignment.Center),
            icon = R.drawable.ic_arrow_down_20,
            onClick = onSwitchPairs
        )
    }
}

@Composable
private fun SwapCoinInput(
    coinAmount: BigDecimal?,
    fiatAmount: BigDecimal?,
    currency: Currency,
    onValueChange: (BigDecimal?) -> Unit,
    enabled: Boolean = true,
    token: Token?,
    onClickCoin: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AmountInput(coinAmount, onValueChange, enabled)
            VSpacer(height = 3.dp)
            if (fiatAmount != null) {
                body_grey(text = "${currency.symbol}${fiatAmount.toPlainString()}")
            } else {
                body_grey50(text = "${currency.symbol}0")
            }
        }
        HSpacer(width = 8.dp)
        Selector(
            icon = {
                CoinImage(
                    iconUrl = token?.coin?.imageUrl,
                    placeholder = token?.iconPlaceholder,
                    modifier = Modifier.size(32.dp)
                )
            },
            text = {
                if (token != null) {
                    subhead1_leah(text = token.coin.code)
                } else {
                    subhead1_jacob(text = stringResource(R.string.Swap_TokenSelectorTitle))
                }
            },
            onClickSelect = onClickCoin
        )
    }
}

@Composable
private fun Selector(
    icon: @Composable() (RowScope.() -> Unit),
    text: @Composable() (RowScope.() -> Unit),
    onClickSelect: () -> Unit,
) {
    Row(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClickSelect,
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon.invoke(this)
        HSpacer(width = 8.dp)
        text.invoke(this)
        HSpacer(width = 8.dp)
        Icon(
            painter = painterResource(R.drawable.ic_arrow_big_down_20),
            contentDescription = "",
            tint = ComposeAppTheme.colors.grey
        )
    }
}

@Composable
private fun AmountInput(
    value: BigDecimal?,
    onValueChange: (BigDecimal?) -> Unit,
    enabled: Boolean,
) {
    var text by remember(value) {
        mutableStateOf(value?.toPlainString() ?: "")
    }
    BasicTextField(
        modifier = Modifier.fillMaxWidth(),
        value = text,
        onValueChange = {
            try {
                val amount = if (it.isBlank()) {
                    null
                } else {
                    it.toBigDecimal()
                }
                text = it
                onValueChange.invoke(amount)
            } catch (e: Exception) {

            }
        },
        enabled = enabled,
        textStyle = ColoredTextStyle(
            color = ComposeAppTheme.colors.leah, textStyle = ComposeAppTheme.typography.headline1
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
        decorationBox = { innerTextField ->
            if (text.isEmpty()) {
                headline1_grey(text = "0")
            }
            innerTextField()
        },
    )
}
