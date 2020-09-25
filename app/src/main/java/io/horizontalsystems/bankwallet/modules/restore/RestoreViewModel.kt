package io.horizontalsystems.bankwallet.modules.restore

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.core.SingleLiveEvent
import java.lang.Exception

class RestoreViewModel(
        private val service: RestoreModule.IRestoreService,
        private val selectCoins: Boolean,
        private val clearables: List<Clearable>
) : ViewModel() {

    var openScreenLiveEvent = SingleLiveEvent<Screen>()
    var finishLiveEvent = SingleLiveEvent<Unit>()

    val initialScreen: Screen
        get() {
            service.predefinedAccountType?.let {
                return Screen.RestoreAccountType(it)
            }

            return Screen.SelectPredefinedAccountType
        }

    fun onSelect(predefinedAccountType: PredefinedAccountType) {
        service.predefinedAccountType = predefinedAccountType
        openScreenLiveEvent.postValue(Screen.RestoreAccountType(predefinedAccountType))
    }

    fun onEnter(accountType: AccountType) {
        service.accountType = accountType

        if (selectCoins) {
            service.predefinedAccountType?.let {
                openScreenLiveEvent.postValue(Screen.SelectCoins(it))
            }
        } else {
            restore()
        }
    }

    fun onSelect(coins: List<Coin>) {
        restore(coins)
    }

    override fun onCleared() {
        clearables.forEach {
            it.clear()
        }
        super.onCleared()
    }

    private fun restore(coins: List<Coin> = listOf()) {
        try {
            service.restoreAccount(coins)
            finishLiveEvent.call()
        } catch (e: Exception) {
            // restore should not be called before setting account type. No need to handle error
        }
    }

    sealed class Screen {
        object SelectPredefinedAccountType : Screen()
        class RestoreAccountType(val predefinedAccountType: PredefinedAccountType) : Screen()
        class SelectCoins(val predefinedAccountType: PredefinedAccountType) : Screen()
    }
}
