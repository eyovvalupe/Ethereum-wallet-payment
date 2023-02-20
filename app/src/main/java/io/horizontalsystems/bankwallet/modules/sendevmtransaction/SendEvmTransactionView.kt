package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.fee.FeeCell
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmNonceViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.*

@Composable
fun SendEvmTransactionView(
    transactionViewModel: SendEvmTransactionViewModel,
    feeCellViewModel: EvmFeeCellViewModel,
    nonceViewModel: SendEvmNonceViewModel,
    navController: NavController,
    description: String? = null
) {
    ComposeAppTheme {

        val items by transactionViewModel.viewItemsLiveData.observeAsState(listOf())
        val fee by feeCellViewModel.feeLiveData.observeAsState(null)
        val viewState by feeCellViewModel.viewStateLiveData.observeAsState()

        Column {
            description?.let {
                subhead2_grey(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    text = it
                )
            }
            items.forEach { sectionViewItem ->
                SectionView(sectionViewItem.viewItems)
            }

            NonceView(nonceViewModel)

            Spacer(Modifier.height(16.dp))
            CellUniversalLawrenceSection(
                listOf {
                    FeeCell(
                        title = stringResource(R.string.FeeSettings_Fee),
                        info = stringResource(R.string.FeeSettings_Fee_Info),
                        value = fee,
                        viewState = viewState,
                        navController = navController
                    )
                }
            )

            val cautions by transactionViewModel.cautionsLiveData.observeAsState()
            cautions?.let {
                Cautions(it)
            }
        }
    }
}

@Composable
private fun NonceView(nonceViewModel: SendEvmNonceViewModel) {
    if (nonceViewModel.default) return
    val nonce = nonceViewModel.nonce ?: return

    Spacer(Modifier.height(16.dp))
    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                subhead2_grey(
                    text = stringResource(id = R.string.Send_Confirmation_Nonce)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = nonce.toString(),
                    maxLines = 1,
                    style = ComposeAppTheme.typography.subhead1,
                    color = setColorByType(ValueType.Regular)
                )
            }
        }
    )
}

@Composable
private fun SectionView(viewItems: List<ViewItem>) {
    Spacer(Modifier.height(16.dp))
    CellUniversalLawrenceSection(viewItems) { item ->
        when (item) {
            is ViewItem.Subhead -> Subhead(item)
            is ViewItem.Value -> TitleValue(item)
            is ViewItem.ValueMulti -> TitleValueMulti(item)
            is ViewItem.AmountMulti -> AmountMulti(item)
            is ViewItem.Amount -> Amount(item)
            is ViewItem.NftAmount -> NftAmount(item)
            is ViewItem.Address -> TitleValueHex(item.title, item.valueTitle, item.value)
            is ViewItem.Input -> TitleValueHex("Input", item.value.shorten(), item.value)
            is ViewItem.TokenItem -> Token(item)
        }
    }
}

@Composable
private fun Subhead(item: ViewItem.Subhead) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        item.iconRes?.let {
            Icon(
                modifier = Modifier.padding(end = 16.dp),
                painter = painterResource(id = it),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }
        headline2_leah(
            text = item.title
        )
        Spacer(Modifier.weight(1f))
        subhead1_grey(
            text = item.value
        )
    }
}

@Composable
private fun TitleValue(item: ViewItem.Value) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        subhead2_grey(
            text = item.title
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = item.value,
            maxLines = 1,
            style = ComposeAppTheme.typography.subhead1,
            color = setColorByType(item.type)
        )
    }
}

@Composable
private fun TitleValueMulti(item: ViewItem.ValueMulti) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        subhead2_grey(
            text = item.title
        )
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = item.primaryValue,
                maxLines = 1,
                style = ComposeAppTheme.typography.subhead1,
                color = setColorByType(item.type)
            )
            Text(
                text = item.secondaryValue,
                maxLines = 1,
                style = ComposeAppTheme.typography.caption,
                color = ComposeAppTheme.colors.grey
            )
        }
    }
}

@Composable
private fun AmountMulti(item: ViewItem.AmountMulti) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        CoinImage(
            modifier = Modifier.size(32.dp),
            iconUrl = item.token.coin.iconUrl,
            placeholder = item.token.iconPlaceholder
        )
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.amounts[0].coinAmount,
                    maxLines = 1,
                    style = ComposeAppTheme.typography.subhead1,
                    color = setColorByType(item.type)
                )
                Spacer(Modifier.weight(1f))
                subhead2_grey(
                    text = item.amounts[0].fiatAmount ?: ""
                )
            }
            if (item.amounts.size > 1) {
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    caption_grey(
                        text = item.amounts[1].coinAmount
                    )
                    Spacer(Modifier.weight(1f))
                    caption_grey(
                        text = item.amounts[1].fiatAmount ?: ""
                    )
                }
            }
        }
    }
}

@Composable
private fun Amount(item: ViewItem.Amount) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        CoinImage(
            modifier = Modifier
                .padding(end = 16.dp)
                .size(32.dp),
            iconUrl = item.token.coin.iconUrl,
            placeholder = item.token.iconPlaceholder
        )
        Text(
            text = item.coinAmount,
            maxLines = 1,
            style = ComposeAppTheme.typography.subhead1,
            color = setColorByType(item.type)
        )
        Spacer(Modifier.weight(1f))
        subhead2_grey(
            text = item.fiatAmount ?: ""
        )
    }
}

@Composable
private fun NftAmount(item: ViewItem.NftAmount) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        NftIcon(
            modifier = Modifier.padding(end = 16.dp),
            iconUrl = item.iconUrl,
        )
        Text(
            text = item.amount,
            maxLines = 1,
            style = ComposeAppTheme.typography.subhead2,
            color = setColorByType(item.type)
        )
    }
}

@Composable
private fun Token(item: ViewItem.TokenItem) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        CoinImage(
            modifier = Modifier
                .padding(end = 16.dp)
                .size(32.dp),
            iconUrl = item.token.coin.iconUrl,
            placeholder = item.token.iconPlaceholder
        )
        subhead1_leah(item.token.coin.code)
    }
}

@Composable
private fun TitleValueHex(
    title: String,
    valueTitle: String,
    value: String,
) {
    val localView = LocalView.current
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        subhead2_grey(
            text = title
        )
        Spacer(Modifier.weight(1f))
        ButtonSecondaryDefault(
            modifier = Modifier.height(28.dp),
            title = valueTitle,
            onClick = {
                TextHelper.copyText(value)
                HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
            }
        )
    }
}

@Composable
private fun setColorByType(type: ValueType) =
    when (type) {
        ValueType.Regular -> ComposeAppTheme.colors.bran
        ValueType.Disabled -> ComposeAppTheme.colors.grey
        ValueType.Outgoing -> ComposeAppTheme.colors.leah
        ValueType.Incoming -> ComposeAppTheme.colors.remus
    }

@Preview
@Composable
private fun Preview_Subhead() {
    val item = ViewItem.Subhead("Title", "Value", R.drawable.ic_arrow_down_left_24)
    ComposeAppTheme {
        Subhead(item)
    }
}

@Preview
@Composable
private fun Preview_TitleValue() {
    val item = ViewItem.Value("Title", "Value", ValueType.Incoming)
    ComposeAppTheme {
        TitleValue(item)
    }
}

@Preview
@Composable
private fun Preview_AmountMulti() {
    val token = Token(
        coin = Coin("uid", "KuCoin", "KCS"),
        blockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null),
        type = TokenType.Eip20("eef"),
        decimals = 18
    )
    val item = ViewItem.AmountMulti(
        listOf(
            AmountValues("0.104 KCS (est)", "$0.99"),
            AmountValues("0.103 KCS (min)", "$0.95"),
        ),
        ValueType.Incoming,
        token
    )
    ComposeAppTheme {
        AmountMulti(item)
    }
}

@Preview
@Composable
private fun Preview_Amount() {
    val token = Token(
        coin = Coin("uid", "KuCoin", "KCS"),
        blockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null),
        type = TokenType.Eip20("eef"),
        decimals = 18
    )
    val item = ViewItem.Amount(
        "$0.99",
        "0.104 KCS (est)",
        ValueType.Outgoing,
        token
    )
    ComposeAppTheme {
        Amount(item)
    }
}

@Preview
@Composable
private fun Preview_TitleValueHex() {
    ComposeAppTheme {
        TitleValueHex("Title", "ValueShort", "ValueLong")
    }
}
