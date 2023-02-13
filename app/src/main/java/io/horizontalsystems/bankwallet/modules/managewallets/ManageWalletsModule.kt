package io.horizontalsystems.bankwallet.modules.managewallets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.enablecoin.EnableCoinServiceXxx
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinTokensService
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinTokensViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsService
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsService
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel

object ManageWalletsModule {

    class Factory : ViewModelProvider.Factory {

        private val restoreSettingsService by lazy {
            RestoreSettingsService(App.restoreSettingsManager, App.zcashBirthdayProvider)
        }

        private val coinSettingsService by lazy {
            CoinSettingsService()
        }

        private val coinTokensService by lazy {
            CoinTokensService()
        }

        private val enableCoinService by lazy {
            EnableCoinServiceXxx(coinTokensService, restoreSettingsService, coinSettingsService)
        }

        private val manageWalletsService by lazy {
            ManageWalletsService(
                App.marketKit,
                App.walletManager,
                App.accountManager,
                enableCoinService,
                App.restoreSettingsManager,
            )
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                RestoreSettingsViewModel::class.java -> {
                    RestoreSettingsViewModel(restoreSettingsService, listOf(restoreSettingsService)) as T
                }
                CoinSettingsViewModel::class.java -> {
                    CoinSettingsViewModel(coinSettingsService, listOf(coinSettingsService)) as T
                }
                ManageWalletsViewModel::class.java -> {
                    ManageWalletsViewModel(manageWalletsService, listOf(manageWalletsService)) as T
                }
                CoinTokensViewModel::class.java -> {
                    CoinTokensViewModel(coinTokensService, App.accountManager) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}
