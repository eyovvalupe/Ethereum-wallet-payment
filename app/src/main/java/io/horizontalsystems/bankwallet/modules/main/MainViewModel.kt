package io.horizontalsystems.bankwallet.modules.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.RateUsType
import io.horizontalsystems.bankwallet.core.managers.ReleaseNotesManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel(
        private val pinComponent: IPinComponent,
        rateAppManager: IRateAppManager,
        private val backupManager: IBackupManager,
        private val termsManager: ITermsManager,
        accountManager: IAccountManager,
        private val releaseNotesManager: ReleaseNotesManager,
        service: MainService
) : ViewModel() {

    val showRootedDeviceWarningLiveEvent = SingleLiveEvent<Unit>()
    val showRateAppLiveEvent = SingleLiveEvent<Unit>()
    val showWhatsNewLiveEvent = SingleLiveEvent<String>()
    val openPlayMarketLiveEvent = SingleLiveEvent<Unit>()
    val hideContentLiveData = MutableLiveData<Boolean>()
    val setBadgeVisibleLiveData = MutableLiveData<Boolean>()
    val transactionTabEnabledLiveData = MutableLiveData<Boolean>()

    private val disposables = CompositeDisposable()
    private var contentHidden = pinComponent.isLocked

    init {

        if (!service.ignoreRootCheck && service.isDeviceRooted) {
            showRootedDeviceWarningLiveEvent.call()
        }

        updateBadgeVisibility()
        sync(accountManager.accounts)

        disposables.add(backupManager.allBackedUpFlowable.subscribe {
            updateBadgeVisibility()
        })

        disposables.add(termsManager.termsAcceptedSignal.subscribe {
            updateBadgeVisibility()
        })

        disposables.add(pinComponent.pinSetFlowable.subscribe {
            updateBadgeVisibility()
        })

        disposables.add(accountManager.accountsFlowable.subscribe {
            sync(it)
        })

        rateAppManager.showRateAppObservable
                .subscribe {
                    showRateApp(it)
                }
                .let {
                    disposables.add(it)
                }

        showWhatsNew()
    }

    override fun onCleared() {
        disposables.clear()
    }

    fun onResume() {
        if (contentHidden != pinComponent.isLocked) {
            hideContentLiveData.postValue(pinComponent.isLocked)
        }
        contentHidden = pinComponent.isLocked
    }

    fun sync(accounts: List<Account>) {
        transactionTabEnabledLiveData.postValue(accounts.isNotEmpty())
    }

    private fun showWhatsNew() {
        viewModelScope.launch{
            if (releaseNotesManager.shouldShowChangeLog()){
                delay(2000)
                showWhatsNewLiveEvent.postValue(releaseNotesManager.releaseNotesUrl)
            }
        }
    }

    private fun showRateApp(showRateUs: RateUsType) {
        when (showRateUs) {
            RateUsType.OpenPlayMarket -> openPlayMarketLiveEvent.postValue(Unit)
            RateUsType.ShowDialog -> showRateAppLiveEvent.postValue(Unit)
        }
    }

    private fun updateBadgeVisibility() {
        val visible = !(backupManager.allBackedUp && termsManager.termsAccepted && pinComponent.isPinSet)
        setBadgeVisibleLiveData.postValue(visible)
    }

}
