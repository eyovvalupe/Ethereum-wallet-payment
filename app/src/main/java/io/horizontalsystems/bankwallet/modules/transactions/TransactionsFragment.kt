package io.horizontalsystems.bankwallet.modules.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import kotlinx.coroutines.launch

class TransactionsFragment : BaseFragment() {

    private val viewModel by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment) { TransactionsModule.Factory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    TransactionsScreen(viewModel, findNavController())
                }
            }
        }
    }
}


@Composable
private fun TransactionsScreen(viewModel: TransactionsViewModel, navController: NavController) {
    val filterCoins by viewModel.filterCoinsLiveData.observeAsState()
    val filterTypes by viewModel.filterTypesLiveData.observeAsState()
    val transactions by viewModel.transactionList.observeAsState()
    val viewState by viewModel.viewState.observeAsState()
    val syncing by viewModel.syncingLiveData.observeAsState(false)
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.ResString(R.string.Transactions_Title),
                showSpinner = syncing
            )
            filterTypes?.let { filterTypes ->
                FilterTypeTabs(
                    filterTypes,
                    { viewModel.setFilterTransactionType(it) },
                    { scrollToTopAfterUpdate = true })
            }
            filterCoins?.let { filterCoins ->
                FilterCoinTabs(
                    filterCoins,
                    { viewModel.setFilterCoin(it) },
                    { scrollToTopAfterUpdate = true })
            }

            Crossfade(viewState) { viewState ->
                when (viewState) {
                    ViewState.Success -> {
                        transactions?.let { transactionItems ->
                            if (transactionItems.isEmpty()) {
                                if (syncing) {
                                    ListEmptyView(
                                        text = stringResource(R.string.Transactions_WaitForSync),
                                        icon = R.drawable.ic_clock
                                    )
                                } else {
                                    ListEmptyView(
                                        text = stringResource(R.string.Transactions_EmptyList),
                                        icon = R.drawable.ic_outgoingraw
                                    )
                                }
                            } else {
                                TransactionList(
                                    transactionItems,
                                    scrollToTopAfterUpdate,
                                    { viewModel.willShow(it) },
                                    { onTransactionClick(it, viewModel, navController) },
                                    { viewModel.onBottomReached() }
                                )
                                if (scrollToTopAfterUpdate) {
                                    scrollToTopAfterUpdate = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun onTransactionClick(
    transactionViewItem: TransactionViewItem,
    viewModel: TransactionsViewModel,
    navController: NavController
) {
    val transactionItem = viewModel.getTransactionItem(transactionViewItem) ?: return

    viewModel.tmpItemToShow = transactionItem

    navController.slideFromBottom(R.id.transactionInfoFragment)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionList(
    transactionsMap: Map<String, List<TransactionViewItem>>,
    scrollToTop: Boolean,
    willShow: (TransactionViewItem) -> Unit,
    onClick: (TransactionViewItem) -> Unit,
    onBottomReached: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val bottomReachedUid = getBottomReachedUid(transactionsMap)

    LazyColumn(state = listState) {
        transactionsMap.forEach { (dateHeader, transactions) ->
            stickyHeader {
                DateHeader(dateHeader)
            }

            items(transactions) { item ->
                TransactionCell(item) { onClick.invoke(item) }

                willShow.invoke(item)

                if (item.uid == bottomReachedUid) {
                    onBottomReached.invoke()
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (scrollToTop) {
            coroutineScope.launch {
                listState.scrollToItem(0)
            }
        }
    }
}

private fun getBottomReachedUid(transactionsMap: Map<String, List<TransactionViewItem>>): String? {
    val txList = transactionsMap.values.flatten()
    //get index not exact bottom but near to the bottom, to make scroll smoother
    val index = if (txList.size > 4) txList.size - 4 else 0

    return txList.getOrNull(index)?.uid
}

@Composable
fun DateHeader(dateHeader: String) {
    Header(borderTop = false, borderBottom = true) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = dateHeader,
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead1,
            maxLines = 1,
        )
    }
}

@Composable
fun TransactionCell(item: TransactionViewItem, onClick: () -> Unit) {
    CellMultilineClear(borderBottom = true, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(40.dp)
            ) {
                item.progress?.let { progress ->
                    CircularProgressIndicator(
//                        progress = progress / 100f,
                        color = ComposeAppTheme.colors.grey50,
                        strokeWidth = 2.dp
                    )
                }
                val icon = item.icon
                when (icon) {
                    TransactionViewItem.Icon.Failed -> {
                        Icon(
                            modifier = Modifier.align(Alignment.Center),
                            painter = painterResource(R.drawable.ic_attention_24),
                            tint = ComposeAppTheme.colors.lucian,
                            contentDescription = null
                        )
                    }
                    is TransactionViewItem.Icon.Platform -> {
                        Icon(
                            modifier = Modifier.align(Alignment.Center),
                            painter = painterResource(icon.iconRes ?: R.drawable.coin_placeholder),
                            tint = ComposeAppTheme.colors.leah,
                            contentDescription = null
                        )
                    }
                    is TransactionViewItem.Icon.Regular -> {
                        CoinImage(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(24.dp),
                            iconUrl = icon.url,
                            placeholder = icon.placeholder
                        )
                    }
                    is TransactionViewItem.Icon.Swap -> {
                        CoinImage(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 6.dp, start = 6.dp)
                                .size(20.dp),
                            iconUrl = icon.iconIn.url,
                            placeholder = icon.iconIn.placeholder,
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 6.5.dp, end = 6.5.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(ComposeAppTheme.colors.tyler)
                        )

                        CoinImage(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 6.dp, end = 6.dp)
                                .size(20.dp),
                            iconUrl = icon.iconOut.url,
                            placeholder = icon.iconOut.placeholder,
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(end = 16.dp)) {
                Row {
                    Text(
                        text = item.title,
                        color = ComposeAppTheme.colors.leah,
                        style = ComposeAppTheme.typography.body,
                        maxLines = 1,
                    )
                    Spacer(Modifier.weight(1f))
                    item.primaryValue?.let { coloredValue ->
                        Text(
                            text = coloredValue.value,
                            style = ComposeAppTheme.typography.body,
                            color = coloredValue.color.compose(),
                            maxLines = 1,
                        )
                    }
                    if (item.doubleSpend) {
                        Image(
                            modifier = Modifier.padding(start = 6.dp),
                            painter = painterResource(R.drawable.ic_double_spend_20),
                            contentDescription = null
                        )
                    }
                    item.locked?.let { locked ->
                        Image(
                            modifier = Modifier.padding(start = 6.dp),
                            painter = painterResource(if (locked) R.drawable.ic_lock_20 else R.drawable.ic_unlock_20),
                            contentDescription = null
                        )
                    }
                    if (item.sentToSelf) {
                        Image(
                            modifier = Modifier.padding(start = 6.dp),
                            painter = painterResource(R.drawable.ic_arrow_return_20),
                            contentDescription = null
                        )
                    }
                }
                Spacer(Modifier.height(1.dp))
                Row {
                    Text(
                        text = item.subtitle,
                        color = ComposeAppTheme.colors.grey,
                        style = ComposeAppTheme.typography.subhead2,
                        maxLines = 1,
                    )
                    Spacer(Modifier.weight(1f))
                    item.secondaryValue?.let { coloredValue ->
                        Text(
                            text = coloredValue.value,
                            style = ComposeAppTheme.typography.subhead2,
                            color = coloredValue.color.compose(),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTypeTabs(
    filterTypes: List<Filter<FilterTransactionType>>,
    onTransactionTypeClick: (FilterTransactionType) -> Unit,
    scrollToTopAfterUpdate: () -> Unit
) {
    val tabItems = filterTypes.map {
        TabItem(stringResource(it.item.title), it.selected, it.item)
    }

    ScrollableTabs(tabItems) { transactionType ->
        onTransactionTypeClick.invoke(transactionType)
        scrollToTopAfterUpdate.invoke()
    }
}

@Composable
private fun FilterCoinTabs(
    filterCoins: List<Filter<TransactionWallet>>,
    onCoinFilterClick: (TransactionWallet?) -> Unit,
    scrollToTopAfterUpdate: () -> Unit
) {
    val tabItems = filterCoins.mapNotNull {
        it.item.platformCoin?.let { platformCoin ->
            TabItem(
                platformCoin.code,
                it.selected,
                it.item,
                ImageSource.Remote(
                    platformCoin.coin.iconUrl,
                    platformCoin.coinType.iconPlaceholder
                ),
                it.item.badge
            )
        }
    }

    CardTabs(tabItems = tabItems, edgePadding = 16.dp) {
        onCoinFilterClick.invoke(it)
        scrollToTopAfterUpdate.invoke()
    }
}

data class Filter<T>(val item: T, val selected: Boolean)
