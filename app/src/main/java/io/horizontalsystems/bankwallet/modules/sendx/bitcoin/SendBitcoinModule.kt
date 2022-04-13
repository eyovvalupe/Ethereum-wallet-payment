package io.horizontalsystems.bankwallet.modules.sendx.bitcoin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.entities.Wallet

object SendBitcoinModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapter = App.adapterManager.getAdapterForWallet(wallet) as ISendBitcoinAdapter

            val provider = FeeRateProviderFactory.provider(wallet.coinType)!!
            val feeService = SendBitcoinFeeService(adapter)
            val feeRateService = SendBitcoinFeeRateService(provider)
            val amountService = SendBitcoinAmountService(adapter, wallet.coin.code)
            val addressService = SendBitcoinAddressService(adapter)
            val pluginService = SendBitcoinPluginService(App.localStorage, wallet.coinType)
            return SendBitcoinViewModel(
                adapter,
                wallet,
                feeRateService,
                feeService,
                amountService,
                addressService,
                pluginService,
                App.localStorage.transactionSortingType
            )  as T
        }
    }
}
