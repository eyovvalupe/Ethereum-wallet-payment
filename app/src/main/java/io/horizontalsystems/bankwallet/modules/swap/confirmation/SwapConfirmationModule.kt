package io.horizontalsystems.bankwallet.modules.swap.confirmation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ethereum.CoinService
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.modules.swap.SwapService
import io.horizontalsystems.bankwallet.modules.swap.SwapTradeService
import io.horizontalsystems.bankwallet.modules.swap.SwapViewItemHelper

object SwapConfirmationModule {

    class Factory(
            private val service: SwapService,
            private val tradeService: SwapTradeService,
            private val transactionService: EvmTransactionService
    ) : ViewModelProvider.Factory {

        private val coinService by lazy { CoinService(service.dex.coin, App.currencyManager, App.xRateManager) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val stringProvider = StringProvider()
            val formatter = SwapViewItemHelper(stringProvider, App.numberFormatter)

            return SwapConfirmationViewModel(service, tradeService, transactionService, coinService, App.numberFormatter, formatter, stringProvider) as T
        }
    }

}