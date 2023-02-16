package io.horizontalsystems.bankwallet.modules.coin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.navGraphViewModels
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.coinmarkets.CoinMarketsScreen
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsScreen
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.CoinOverviewScreen
import io.horizontalsystems.bankwallet.modules.coin.tweets.CoinTweetsScreen
import io.horizontalsystems.bankwallet.modules.profeatures.yakauthorization.YakAuthorizationModule
import io.horizontalsystems.bankwallet.modules.profeatures.yakauthorization.YakAuthorizationViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorMultipleDialog
import io.horizontalsystems.core.CustomSnackbar
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch

class CoinFragment : BaseFragment() {
    private val authorizationViewModel by navGraphViewModels<YakAuthorizationViewModel>(R.id.coinFragment) { YakAuthorizationModule.Factory() }

    private var snackbarInProcess: CustomSnackbar? = null

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
                val uid = try {
                    activity?.intent?.data?.getQueryParameter("uid")
                } catch (e: UnsupportedOperationException) {
                    null
                }

                val coinUid = requireArguments().getString(COIN_UID_KEY, uid ?: "")
                if (uid != null) {
                    activity?.intent?.data = null
                }

                CoinScreen(
                    coinUid,
                    coinViewModel(coinUid),
                    authorizationViewModel,
                    findNavController(),
                    childFragmentManager
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snackbarInProcess?.dismiss()
        snackbarInProcess = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        authorizationViewModel.stateLiveData.observe(viewLifecycleOwner) { state ->
//            when (state) {
//                YakAuthorizationService.State.Idle ->
//                    snackbarInProcess?.dismiss()
//
//                YakAuthorizationService.State.Authenticating ->
//                    snackbarInProcess = HudHelper.showInProcessMessage(
//                        requireView(),
//                        R.string.ProUsersInfo_Features_Authenticating,
//                        SnackbarDuration.INDEFINITE
//                    )
//
//                YakAuthorizationService.State.NoYakNft -> {
//                    snackbarInProcess?.dismiss()
//                    findNavController().slideFromBottom(
//                        R.id.proUsersInfoDialog
//                    )
//                }
//
//                YakAuthorizationService.State.Authenticated -> {}
//
//                is YakAuthorizationService.State.SignMessageReceived -> {
//                    snackbarInProcess?.dismiss()
//                    findNavController().slideFromBottom(
//                        R.id.proUsersActivateDialog
//                    )
//                }
//
//                is YakAuthorizationService.State.Failed ->
//                    snackbarInProcess = HudHelper.showErrorMessage(
//                        requireView(),
//                        state.exception.toString()
//                    )
//            }
//        }
    }

    private fun coinViewModel(coinUid: String): CoinViewModel? = try {
        val viewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment) {
            CoinModule.Factory(coinUid)
        }
        viewModel
    } catch (e: Exception) {
        null
    }

    private fun showBottomSelectorDialog(
        config: BottomSheetSelectorMultipleDialog.Config,
        onSelect: (indexes: List<Int>) -> Unit,
        onCancel: () -> Unit
    ) {
        BottomSheetSelectorMultipleDialog.show(
            fragmentManager = childFragmentManager,
            title = config.title,
            icon = config.icon,
            items = config.viewItems,
            selected = config.selectedIndexes,
            onItemSelected = { onSelect(it) },
            onCancelled = { onCancel() },
            warningTitle = config.descriptionTitle,
            warning = config.description,
            notifyUnchanged = true,
            allowEmpty = config.allowEmpty
        )
    }

    companion object {
        private const val COIN_UID_KEY = "coin_uid_key"

        fun prepareParams(coinUid: String) = bundleOf(COIN_UID_KEY to coinUid)
    }
}

@Composable
fun CoinScreen(
    coinUid: String,
    coinViewModel: CoinViewModel?,
    authorizationViewModel: YakAuthorizationViewModel,
    navController: NavController,
    fragmentManager: FragmentManager
) {
    ComposeAppTheme {
        if (coinViewModel != null) {
            CoinTabs(coinViewModel, authorizationViewModel, navController, fragmentManager)
        } else {
            CoinNotFound(coinUid, navController)
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun CoinTabs(
    viewModel: CoinViewModel,
    authorizationViewModel: YakAuthorizationViewModel,
    navController: NavController,
    fragmentManager: FragmentManager
) {
    val pagerState = rememberPagerState(initialPage = 0)
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = TranslatableString.PlainString(viewModel.fullCoin.coin.code),
            navigationIcon = {
                HsBackButton(onClick = { navController.popBackStack() })
            },
            menuItems = buildList {
                if (viewModel.isWatchlistEnabled) {
                    if (viewModel.isFavorite) {
                        add(
                            MenuItem(
                                title = TranslatableString.ResString(R.string.CoinPage_Unfavorite),
                                icon = R.drawable.ic_filled_star_24,
                                tint = ComposeAppTheme.colors.jacob,
                                onClick = { viewModel.onUnfavoriteClick() }
                            )
                        )
                    } else {
                        add(
                            MenuItem(
                                title = TranslatableString.ResString(R.string.CoinPage_Favorite),
                                icon = R.drawable.ic_star_24,
                                onClick = { viewModel.onFavoriteClick() }
                            )
                        )
                    }
                }
            }
        )

        val tabs = viewModel.tabs
        val selectedTab = tabs[pagerState.currentPage]
        val tabItems = tabs.map {
            TabItem(stringResource(id = it.titleResId), it == selectedTab, it)
        }
        ScrollableTabs(tabItems, onClick = {
            coroutineScope.launch {
                pagerState.scrollToPage(it.ordinal)
            }
        })

        HorizontalPager(
            count = tabs.size,
            state = pagerState,
            userScrollEnabled = false
        ) { page ->
            when (tabs[page]) {
                CoinModule.Tab.Overview -> {
                    CoinOverviewScreen(
                        fullCoin = viewModel.fullCoin,
                        navController = navController
                    )
                }
                CoinModule.Tab.Market -> {
                    CoinMarketsScreen(fullCoin = viewModel.fullCoin)
                }
                CoinModule.Tab.Details -> {
                    CoinDetailsScreen(
                        fullCoin = viewModel.fullCoin,
                        authorizationViewModel = authorizationViewModel,
                        navController = navController,
                        fragmentManager = fragmentManager
                    )
                }
                CoinModule.Tab.Tweets -> {
                    CoinTweetsScreen(fullCoin = viewModel.fullCoin)
                }
            }
        }

        viewModel.successMessage?.let {
            HudHelper.showSuccessMessage(view, it)

            viewModel.onSuccessMessageShown()
        }
    }
}

@Composable
fun CoinNotFound(coinUid: String, navController: NavController) {
    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = TranslatableString.PlainString(coinUid),
            navigationIcon = {
                HsBackButton(onClick = { navController.popBackStack() })
            }
        )

        ListEmptyView(
            text = stringResource(R.string.CoinPage_CoinNotFound, coinUid),
            icon = R.drawable.ic_not_available
        )

    }
}
