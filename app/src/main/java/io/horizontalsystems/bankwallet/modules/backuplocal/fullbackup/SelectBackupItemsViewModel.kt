package io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.ViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SelectBackupItemsViewModel(
    private val backupProvider: BackupProvider,
) : ViewModel() {

    private var viewState: ViewState = ViewState.Loading
    private var wallets: List<WalletBackupViewItem> = emptyList()
    private var otherBackupItems: List<BackupItem> = emptyList()

    var uiState by mutableStateOf(
        UIState(
            viewState = viewState,
            wallets = wallets,
            otherBackupItems = otherBackupItems
        )
    )
        private set

    val selectedWallets: List<String>
        get() = wallets.filter { it.selected }.map { it.account.id }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val backupItems = backupProvider.fullBackupItems()
            wallets = backupItems.wallets.map { account ->
                WalletBackupViewItem(
                    account = account,
                    name = account.name,
                    type = account.type.detailedDescription,
                    backupRequired = !account.isBackedUp && !account.isFileBackedUp,
                    selected = true
                )
            }
            otherBackupItems = backupItems.others
            viewState = ViewState.Success

            emitState()
        }
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = UIState(viewState, wallets, otherBackupItems)
        }
    }

    fun toggle(wallet: WalletBackupViewItem) {
        wallets = wallets.map {
            if (wallet.account.id == it.account.id) {
                it.copy(selected = !wallet.selected)
            } else {
                it
            }
        }

        emitState()
    }

    data class UIState(
        val viewState: ViewState,
        val wallets: List<WalletBackupViewItem>,
        val otherBackupItems: List<BackupItem>
    )

    data class WalletBackupViewItem(
        val account: Account,
        val name: String,
        val type: String,
        val backupRequired: Boolean,
        val selected: Boolean
    )

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SelectBackupItemsViewModel(App.backupProvider) as T
        }
    }

}
