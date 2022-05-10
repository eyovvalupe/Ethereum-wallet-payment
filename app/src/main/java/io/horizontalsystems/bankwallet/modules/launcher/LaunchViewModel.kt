package io.horizontalsystems.bankwallet.modules.launcher

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.core.IKeyStoreManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.core.security.KeyStoreValidationResult

class LaunchViewModel(
    private val accountManager: IAccountManager,
    private val pinComponent: IPinComponent,
    private val systemInfoManager: ISystemInfoManager,
    private val keyStoreManager: IKeyStoreManager,
    localStorage: ILocalStorage
) : ViewModel() {
    val openWelcomeModule = SingleLiveEvent<Void>()
    val openMainModule = SingleLiveEvent<Void>()
    val openUnlockModule = SingleLiveEvent<Void>()
    val openNoSystemLockModule = SingleLiveEvent<Void>()
    val openKeyInvalidatedModule = SingleLiveEvent<Void>()
    val openUserAuthenticationModule = SingleLiveEvent<Void>()
    val closeApplication = SingleLiveEvent<Void>()

    private val mainShowedOnce = localStorage.mainShowedOnce

    init {
        if (systemInfoManager.isSystemLockOff) {
            openNoSystemLockModule()
        } else {
            when (keyStoreManager.validateKeyStore()) {
                KeyStoreValidationResult.UserNotAuthenticated -> {
                    openUserAuthenticationModule()
                }
                KeyStoreValidationResult.KeyIsInvalid -> {
                    openKeyInvalidatedModule()
                }
                KeyStoreValidationResult.KeyIsValid -> {
                    when {
                        accountManager.isAccountsEmpty && !mainShowedOnce -> openWelcomeModule()
                        pinComponent.isLocked -> openUnlockModule()
                        else -> openMainModule()
                    }
                }
            }
        }
    }

    private fun openWelcomeModule() {
        openWelcomeModule.call()
    }

    private fun openMainModule() {
        openMainModule.call()
    }

    private fun openUnlockModule() {
        openUnlockModule.call()
    }

    private fun openNoSystemLockModule() {
        openNoSystemLockModule.call()
    }

    private fun openKeyInvalidatedModule() {
        openKeyInvalidatedModule.call()
    }

    private fun openUserAuthenticationModule() {
        openUserAuthenticationModule.call()
    }

    private fun closeApplication() {
        closeApplication.call()
    }

    fun didUnlock() {
        openMainModule()
    }

    fun didCancelUnlock() {
        closeApplication()
    }
}
