package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CommunicationMode
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.core.SingleLiveEvent

class PrivacySettingsViewModel : ViewModel(), PrivacySettingsModule.IPrivacySettingsView, PrivacySettingsModule.IPrivacySettingsRouter {
    lateinit var delegate: PrivacySettingsModule.IPrivacySettingsViewDelegate

    val torEnabledLiveData = MutableLiveData<Boolean>()
    val showAppRestartAlertForTor = SingleLiveEvent<Boolean>()
    val showNotificationsNotEnabledAlert = SingleLiveEvent<Unit>()
    val communicationSettingsViewItems = SingleLiveEvent<List<PrivacySettingsViewItem>>()
    val restoreWalletSettingsViewItems = SingleLiveEvent<List<PrivacySettingsViewItem>>()
    val showSyncModeSelectorDialog = SingleLiveEvent<Pair<List<SyncMode>, SyncMode>>()
    val showCommunicationSelectorDialog = SingleLiveEvent<Pair<List<CommunicationMode>, CommunicationMode>>()
    val showRestoreModeChangeAlert = SingleLiveEvent<Pair<Coin, SyncMode>>()
    val showCommunicationModeChangeAlert = SingleLiveEvent<Pair<Coin, CommunicationMode>>()

    val restartApp = SingleLiveEvent<Unit>()

    fun init() {
        PrivacySettingsModule.init(this, this)
        delegate.viewDidLoad()
    }

    // IView

    override fun showNotificationsNotEnabledAlert() {
        showNotificationsNotEnabledAlert.call()
    }

    override fun toggleTorEnabled(torEnabled: Boolean) {
        torEnabledLiveData.postValue(torEnabled)
    }

    override fun showRestartAlert(checked: Boolean) {
        showAppRestartAlertForTor.postValue(checked)
    }

    override fun setCommunicationSettingsViewItems(items: List<PrivacySettingsViewItem>) {
        communicationSettingsViewItems.postValue(items)
    }

    override fun setRestoreWalletSettingsViewItems(items: List<PrivacySettingsViewItem>) {
        restoreWalletSettingsViewItems.postValue(items)
    }

    override fun showCommunicationSelectorDialog(communicationModeOptions: List<CommunicationMode>, selected: CommunicationMode) {
        showCommunicationSelectorDialog.postValue(Pair(communicationModeOptions, selected))
    }

    override fun showSyncModeSelectorDialog(syncModeOptions: List<SyncMode>, selected: SyncMode) {
        showSyncModeSelectorDialog.postValue(Pair(syncModeOptions, selected))
    }

    override fun showRestoreModeChangeAlert(coin: Coin, selectedSyncMode: SyncMode) {
        showRestoreModeChangeAlert.postValue(Pair(coin, selectedSyncMode))
    }

    override fun showCommunicationModeChangeAlert(coin: Coin, selectedCommunication: CommunicationMode) {
        showCommunicationModeChangeAlert.postValue(Pair(coin, selectedCommunication))
    }

    // IRouter

    override fun restartApp() {
        restartApp.call()
    }

}
