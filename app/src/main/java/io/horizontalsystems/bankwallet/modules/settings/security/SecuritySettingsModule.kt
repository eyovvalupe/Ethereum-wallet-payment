package io.horizontalsystems.bankwallet.modules.settings.security

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App

object SecuritySettingsModule {

    interface ISecuritySettingsView {
        fun togglePinSet(pinSet: Boolean)
        fun setEditPinVisible(visible: Boolean)
        fun setBiometricSettingsVisible(visible: Boolean)
        fun toggleBiometricEnabled(enabled: Boolean)
    }

    interface ISecuritySettingsViewDelegate {
        fun viewDidLoad()
        fun didTapEditPin()
        fun didSwitchPinSet(enable: Boolean)
        fun didSwitchBiometricEnabled(enable: Boolean)
        fun didSetPin()
        fun didCancelSetPin()
        fun didUnlockPinToDisablePin()
        fun didCancelUnlockPinToDisablePin()
        fun didTapPrivacy()
    }

    interface ISecuritySettingsInteractor {
        val isBiometricAuthSupported: Boolean
        val isPinSet: Boolean
        var isBiometricAuthEnabled: Boolean

        fun disablePin()
    }

    interface ISecuritySettingsRouter {
        fun showEditPin()
        fun showSetPin()
        fun showUnlockPin()
        fun openPrivacySettings()
    }

    fun start(activity: FragmentActivity) {
        activity.supportFragmentManager.commit {
            add(R.id.fragmentContainerView, SecuritySettingsFragment())
            addToBackStack(null)
        }
    }

    fun init(view: SecuritySettingsViewModel, router: ISecuritySettingsRouter) {
        val interactor = SecuritySettingsInteractor(App.systemInfoManager, App.pinComponent)
        val presenter = SecuritySettingsPresenter(router, interactor)

        view.delegate = presenter
        presenter.view = view
    }
}
