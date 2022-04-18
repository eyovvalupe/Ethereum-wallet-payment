package io.horizontalsystems.bankwallet.modules.sendx.zcash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendZcashAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.sendx.AmountValidator
import io.horizontalsystems.bankwallet.modules.xrate.XRateService

object SendZCashModule {

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapter = App.adapterManager.getAdapterForWallet(wallet) as ISendZcashAdapter
            val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
            val amountService = SendZCashAmountService(adapter, AmountValidator(), wallet.coin.code)
            val addressService = SendZCashAddressService(adapter)
            val memoService = SendZCashMemoService()

            return SendZCashViewModel(adapter, wallet, xRateService, amountService, addressService, memoService) as T
        }
    }
}
