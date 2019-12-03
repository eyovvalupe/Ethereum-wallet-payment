package io.horizontalsystems.bankwallet.modules.settings.managekeys

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper

class ManageKeysViewModel : ViewModel(), ManageKeysModule.View, ManageKeysModule.Router {

    val showItemsEvent = SingleLiveEvent<List<ManageAccountItem>>()
    val showErrorEvent = SingleLiveEvent<Exception>()
    val confirmUnlinkEvent = SingleLiveEvent<ManageAccountItem>()
    val confirmBackupEvent = SingleLiveEvent<ManageAccountItem>()
    val showCreateWalletLiveEvent = SingleLiveEvent<PredefinedAccountType>()
    val showCoinRestoreLiveEvent = SingleLiveEvent<PredefinedAccountType>()
    val showBackupModule = SingleLiveEvent<Pair<Account, PredefinedAccountType>>()
    val closeLiveEvent = SingleLiveEvent<Void>()

    lateinit var delegate: ManageKeysModule.ViewDelegate

    fun init() {
        ManageKeysModule.init(this, this)
        delegate.viewDidLoad()
    }

    //  IView

    override fun show(items: List<ManageAccountItem>) {
        showItemsEvent.postValue(items)
    }

    override fun showBackupConfirmation(accountItem: ManageAccountItem) {
        confirmBackupEvent.postValue(accountItem)
    }

    override fun showUnlinkConfirmation(accountItem: ManageAccountItem) {
        confirmUnlinkEvent.value = accountItem
    }

    override fun showSuccess() {
        HudHelper.showSuccessMessage(R.string.Hud_Text_Done, 500)
    }

    override fun showError(error: Exception) {
        showErrorEvent.postValue(error)
    }

    //  Router

    override fun showCreateWallet(predefinedAccountType: PredefinedAccountType) {
        showCreateWalletLiveEvent.postValue(predefinedAccountType)
    }

    override fun showCoinRestore(predefinedAccountType: PredefinedAccountType) {
        showCoinRestoreLiveEvent.postValue(predefinedAccountType)
    }

    override fun showBackup(account: Account, predefinedAccountType: PredefinedAccountType) {
        showBackupModule.postValue(Pair(account, predefinedAccountType))
    }

    override fun close() {
        closeLiveEvent.call()
    }

    //  ViewModel

    override fun onCleared() {
        delegate.onClear()
    }
}
