package io.horizontalsystems.bankwallet.modules.receive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString.ResString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class ReceiveFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            try {
                val viewModel by viewModels<ReceiveViewModel> {
                    ReceiveModule.Factory(
                        arguments?.getParcelable(WALLET_KEY)!!
                    )
                }
                setContent {
                    ReceiveScreen(
                        viewModel,
                        findNavController()
                    )
                }
            } catch (t: Throwable) {
                HudHelper.showErrorMessage(requireView(), t.message ?: t.javaClass.simpleName)
                findNavController().popBackStack()
            }
        }
    }

    companion object {
        const val WALLET_KEY = "wallet_key"
    }

}

@Composable
private fun ReceiveScreen(
    viewModel: ReceiveViewModel,
    navController: NavController,
) {
    val localView = LocalView.current
    val context = LocalContext.current
    val fullCoin = viewModel.wallet.platformCoin.fullCoin
    val qrBitmap = TextHelper.getQrCodeBitmap(viewModel.receiveAddress)
    val addressHint = getAddressHint(viewModel.testNet, viewModel.addressType)

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = ResString(R.string.Deposit_Title, fullCoin.coin.code),
                navigationIcon = {
                    CoinImage(
                        iconUrl = fullCoin.coin.iconUrl,
                        placeholder = fullCoin.iconPlaceholder,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .size(24.dp)
                    )
                },
                menuItems = listOf(
                    MenuItem(
                        title = ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = { navController.popBackStack() }
                    )
                )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Box(
                    modifier = Modifier
                        .padding(horizontal = 72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ComposeAppTheme.colors.white)
                        .size(216.dp)

                ) {
                    qrBitmap?.let {
                        Image(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxSize(),
                            bitmap = it.asImageBitmap(),
                            contentScale = ContentScale.FillWidth,
                            contentDescription = null
                        )
                    }
                }

                if (viewModel.testNet) {
                    Image(
                        painter = painterResource(R.drawable.testnet),
                        contentScale = ContentScale.FillWidth,
                        contentDescription = null
                    )
                }

                Text(
                    modifier = Modifier.padding(top = 23.dp),
                    text = addressHint,
                    textAlign = TextAlign.Center,
                    color = if (viewModel.testNet) ComposeAppTheme.colors.lucian else ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.subhead2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    modifier = Modifier
                        .clickable {
                            TextHelper.copyText(viewModel.receiveAddress)
                            HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
                        }
                        .padding(vertical = 12.dp, horizontal = 24.dp),
                    text = viewModel.receiveAddress,
                    textAlign = TextAlign.Center,
                    color = ComposeAppTheme.colors.bran,
                    style = ComposeAppTheme.typography.subhead1,
                )

                Row(
                    modifier = Modifier.width(IntrinsicSize.Max)
                        .padding(top = 11.dp, bottom = 23.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ButtonSecondaryDefault(
                        modifier = Modifier.padding(end = 6.dp),
                        title = stringResource(R.string.Alert_Copy),
                        onClick = {
                            TextHelper.copyText(viewModel.receiveAddress)
                            HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
                        }
                    )
                    ButtonSecondaryDefault(
                        modifier = Modifier.padding(start = 6.dp),
                        title = stringResource(R.string.Deposit_Share),
                        onClick = {
                            ShareCompat.IntentBuilder(context)
                                .setType("text/plain")
                                .setText(viewModel.receiveAddress)
                                .startChooser()
                        }
                    )
                }
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    title = stringResource(R.string.Button_Close),
                    onClick = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun getAddressHint(testNet: Boolean, addressType: String?): String {
    val addressTypeText = if (testNet) "Testnet" else addressType

    val addressHint = when {
        addressType != null -> stringResource(R.string.Deposit_Your_Address) + " ($addressTypeText)"
        else -> stringResource(R.string.Deposit_Your_Address)
    }
    return addressHint
}
