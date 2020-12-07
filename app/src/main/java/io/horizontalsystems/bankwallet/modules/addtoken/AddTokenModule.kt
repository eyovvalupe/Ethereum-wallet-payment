package io.horizontalsystems.bankwallet.modules.addtoken

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.addtoken.bep2.AddBep2TokenBlockchainService
import io.horizontalsystems.bankwallet.modules.addtoken.erc20.AddErc20TokenBlockchainService
import kotlinx.android.parcel.Parcelize

object AddTokenModule {
    class Factory(private val tokenType: TokenType) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val viewModel = when (tokenType) {
                TokenType.Erc20 -> {
                    val blockchainService = AddErc20TokenBlockchainService(App.appConfigTestMode, App.appConfigProvider, App.networkManager)
                    val service = AddTokenService(App.coinManager, blockchainService)
                    AddTokenViewModel(service, R.string.AddErc20Token_Title, R.string.AddErc20Token_ContractAddressHint)
                }
                TokenType.Bep2 -> {
                    val blockchainService = AddBep2TokenBlockchainService(App.appConfigTestMode, App.networkManager)
                    val service = AddTokenService(App.coinManager, blockchainService)
                    AddTokenViewModel(service, R.string.AddBep2Token_Title, R.string.AddBep2Token_TokenSymbolHint)
                }
            }
            return viewModel as T
        }
    }

    data class ViewItem(val coinName: String, val symbol: String, val decimal: Int)

    sealed class State {
        object Idle: State()
        object Loading: State()
        class AlreadyExists(val coin: Coin): State()
        class Fetched(val coin: Coin): State()
        class Failed(val error: Throwable): State()
    }
}

@Parcelize
enum class TokenType(val value: String) : Parcelable {
    Erc20("erc20"),
    Bep2("bep2");
}
