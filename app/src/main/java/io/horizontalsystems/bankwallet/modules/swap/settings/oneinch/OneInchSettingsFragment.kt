package io.horizontalsystems.bankwallet.modules.swap.settings.oneinch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.info.SwapInfoFragment
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchModule
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchSwapViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.RecipientAddressViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsBaseFragment
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSlippageViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.ui.RecipientAddress
import io.horizontalsystems.bankwallet.modules.swap.settings.ui.SlippageAmount
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class OneInchSettingsFragment : SwapSettingsBaseFragment() {

    private val oneInchViewModel by navGraphViewModels<OneInchSwapViewModel>(R.id.swapFragment) {
        OneInchModule.Factory(dex)
    }

    private val vmFactory by lazy {
        OneInchSwapSettingsModule.Factory(oneInchViewModel.tradeService)
    }

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
                    OneInchSettingsScreen(
                        onCloseClick = {
                            findNavController().popBackStack()
                        },
                        onInfoClick = {
                            findNavController().slideFromRight(
                                R.id.swapInfoFragment,
                                SwapInfoFragment.prepareParams(dex)
                            )
                        },
                        dex = dex,
                        factory = vmFactory,
                    )
                }
            }
        }
    }

}

@Composable
private fun OneInchSettingsScreen(
    onCloseClick: () -> Unit,
    onInfoClick: () -> Unit,
    factory: OneInchSwapSettingsModule.Factory,
    dex: SwapMainModule.Dex,
    oneInchSettinsViewModel: OneInchSettingsViewModel = viewModel(factory = factory),
    recipientAddressViewModel: RecipientAddressViewModel = viewModel(factory = factory),
    slippageViewModel: SwapSlippageViewModel = viewModel(factory = factory),
) {
    val (buttonTitle, buttonEnabled) = oneInchSettinsViewModel.buttonState
    val localview = LocalView.current

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                title = TranslatableString.ResString(R.string.SwapSettings_Title),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Info_Title),
                        icon = R.drawable.ic_info_24,
                        onClick = onInfoClick
                    ),
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = onCloseClick
                    )
                )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    RecipientAddress(dex.blockchainType, recipientAddressViewModel)

                    Spacer(modifier = Modifier.height(24.dp))
                    SlippageAmount(slippageViewModel)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = buttonTitle,
                    onClick = {
                        if (oneInchSettinsViewModel.onDoneClick()) {
                            onCloseClick()
                        } else {
                            HudHelper.showErrorMessage(localview, R.string.default_error_msg)
                        }
                    },
                    enabled = buttonEnabled
                )
            }
        }
    }
}
