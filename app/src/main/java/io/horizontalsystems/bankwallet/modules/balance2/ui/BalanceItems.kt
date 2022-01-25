package io.horizontalsystems.bankwallet.modules.balance2.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.shortenedAddress
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.balance.AccountViewItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceHeaderViewItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem
import io.horizontalsystems.bankwallet.modules.balance2.BalanceViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun BalanceItems(
    headerViewItem: BalanceHeaderViewItem,
    balanceViewItems: List<BalanceViewItem>,
    viewModel: BalanceViewModel,
    accountViewItem: AccountViewItem,
    navController: NavController
) {
    Column {
        val context = LocalContext.current
        TabBalance(
            modifier = Modifier
                .clickable {
                    viewModel.onBalanceClick()
                    HudHelper.vibrate(context)
                }
        ) {
            val color = if (headerViewItem.upToDate) {
                ComposeAppTheme.colors.jacob
            } else {
                ComposeAppTheme.colors.yellow50
            }
            Text(
                text = headerViewItem.xBalanceText,
                style = ComposeAppTheme.typography.headline1,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Header(borderTop = true) {
            ButtonSecondaryTransparent(
                title = stringResource(viewModel.sortType.getTitleRes()),
                iconRight = R.drawable.ic_down_arrow_20,
                onClick = {
//                val sortTypes =
//                    listOf(BalanceSortType.Value, BalanceSortType.Name, BalanceSortType.PercentGrowth)
//                val selectorItems = sortTypes.map {
//                    SelectorItem(getString(it.getTitleRes()), it == currentSortType)
//                }
//                SelectorDialog
//                    .newInstance(selectorItems, getString(R.string.Balance_Sort_PopupTitle)) { position ->
//                        viewModel.setSortType(sortTypes[position])
//                        scrollToTopAfterUpdate = true
//                    }
//                    .show(parentFragmentManager, "balance_sort_type_selector")

                }
            )

            Spacer(modifier = Modifier.weight(1f))

            val clipboardManager = LocalClipboardManager.current
            accountViewItem.address?.let { address ->
                ButtonSecondaryDefault(
                    title = address.shortenedAddress(),
                    onClick = {
                        clipboardManager.setText(AnnotatedString(address))
//                    HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
                    }
                )
            }
            if (accountViewItem.manageCoinsAllowed) {
                ButtonSecondaryCircle(
                    icon = R.drawable.ic_manage_2,
                    onClick = {
                        navController.slideFromRight(
                            R.id.mainFragment_to_manageWalletsFragment
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Wallets(balanceViewItems, viewModel, navController)
    }
}


@Composable
fun Wallets(balanceViewItems: List<BalanceViewItem>, viewModel: BalanceViewModel, navController: NavController) {
//    val isRefreshing by viewModel.isRefreshing.observeAsState()
    val isRefreshing = false
//    val account by viewModel.accountLiveData.observeAsState()

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    HSSwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing ?: false),
        onRefresh = {
//            viewModel.onRefresh()
        }
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 18.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(balanceViewItems) { item ->
                BalanceCard(viewItem = item, viewModel, navController)
            }
//            if (scrollToTopAfterUpdate) {
//                scrollToTopAfterUpdate = false
//                coroutineScope.launch {
//                    listState.scrollToItem(0)
//                }
//            }
        }
    }
}


