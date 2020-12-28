package io.horizontalsystems.bankwallet.modules.noaccount

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.core.SingleLiveEvent
import java.lang.Exception

class NoAccountViewModel(
        private val coin: Coin,
        private val service: NoAccountModule.INoAccountService) : ViewModel() {

    val accountCreateSuccessLiveEvent = SingleLiveEvent<Unit>()
    val accountCreateErrorLiveEvent = SingleLiveEvent<Exception>()

    fun onClickCreateKey() {
        try {
            if (coin.type.predefinedAccountType == PredefinedAccountType.Standard) {
                service.resetAddressFormatSettings()
            }

            val account = service.createAccount(coin.type.predefinedAccountType)
            service.save(account)

            accountCreateSuccessLiveEvent.call()
        } catch (e: Exception) {
            accountCreateErrorLiveEvent.postValue(e)
        }
    }

}
