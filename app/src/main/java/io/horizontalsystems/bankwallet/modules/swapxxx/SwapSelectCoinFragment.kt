package io.horizontalsystems.bankwallet.modules.swapxxx

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.modules.swap.coinselect.SelectSwapCoinDialogScreen
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize

class SwapSelectCoinFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.requireInput<Input>()
        SwapSelectCoinScreen(navController, input.otherSelectedToken)
    }

    @Parcelize
    data class Input(val otherSelectedToken: Token?) : Parcelable
}

@Composable
private fun SwapSelectCoinScreen(navController: NavController, otherSelectedToken: Token?) {
    val viewModel = viewModel<SwapSelectCoinViewModel>(
        factory = SwapSelectCoinViewModel.Factory(otherSelectedToken)
    )
    val uiState = viewModel.uiState

    SelectSwapCoinDialogScreen(
        coinBalanceItems = uiState.coinBalanceItems,
        onSearchTextChanged = viewModel::setQuery,
        onClose = navController::popBackStack
    ) {
        navController.setNavigationResultX(it.token)
        navController.popBackStack()
    }
}
