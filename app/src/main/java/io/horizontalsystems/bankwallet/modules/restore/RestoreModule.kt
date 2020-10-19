package io.horizontalsystems.bankwallet.modules.restore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

object RestoreModule {
    interface IRestoreService {
        var predefinedAccountType: PredefinedAccountType?
        var accountType: AccountType?
        fun restoreAccount(coins: List<Coin> = listOf())
    }

    class Factory(private val selectCoins: Boolean, private val predefinedAccountType: PredefinedAccountType? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = RestoreService(predefinedAccountType, App.walletManager, App.accountCreator, App.accountManager)

            return RestoreViewModel(service, selectCoins, listOf(service)) as T
        }
    }
}
