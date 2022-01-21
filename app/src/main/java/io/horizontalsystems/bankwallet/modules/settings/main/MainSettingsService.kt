package io.horizontalsystems.bankwallet.modules.settings.main

import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IBackupManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSessionManager
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.ILanguageManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class MainSettingsService(
    private val localStorage: ILocalStorage,
    private val backupManager: IBackupManager,
    private val languageManager: ILanguageManager,
    private val systemInfoManager: ISystemInfoManager,
    private val currencyManager: ICurrencyManager,
    private val termsManager: ITermsManager,
    private val pinComponent: IPinComponent,
    private val walletConnectSessionManager: WalletConnectSessionManager,
    private val walletConnectManager: WalletConnectManager
) {

    private val backedUpSubject = BehaviorSubject.create<Boolean>()
    val backedUpObservable: Observable<Boolean> get() = backedUpSubject

    private val pinSetSubject = BehaviorSubject.create<Boolean>()
    val pinSetObservable: Observable<Boolean> get() = pinSetSubject

    private val termsAcceptedSubject = BehaviorSubject.create<Boolean>()
    val termsAcceptedObservable: Observable<Boolean> get() = termsAcceptedSubject

    private val baseCurrencySubject = BehaviorSubject.create<Currency>()
    val baseCurrencyObservable: Observable<Currency> get() = baseCurrencySubject

    private val walletConnectSessionCountSubject = BehaviorSubject.create<Int>()
    val walletConnectSessionCountObservable: Observable<Int> get() = walletConnectSessionCountSubject

    private var disposables: CompositeDisposable = CompositeDisposable()

    val appVersion: String
        get() {
            var appVersion = systemInfoManager.appVersion
            if (Translator.getString(R.string.is_release) == "false") {
                appVersion += " (${BuildConfig.VERSION_CODE})"
            }

            return appVersion
        }

    val themeName: Int
        get() = localStorage.currentTheme.getTitle()

    val allBackedUp: Boolean
        get() = backupManager.allBackedUp

    val walletConnectSessionCount: Int
        get() = walletConnectSessionManager.sessions.count()

    val currentLanguageDisplayName: String
        get() = languageManager.currentLanguageName

    val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    val termsAccepted: Boolean
        get() = termsManager.termsAccepted

    val isPinSet: Boolean
        get() = pinComponent.isPinSet

    val launchScreen: LaunchPage
        get() = localStorage.launchPage ?: LaunchPage.Auto

    fun start() {
        disposables.add(backupManager.allBackedUpFlowable.subscribe {
            backedUpSubject.onNext(it)
        })

        disposables.add(walletConnectSessionManager.sessionsObservable.subscribe {
            walletConnectSessionCountSubject.onNext(it.size)
        })

        disposables.add(currencyManager.baseCurrencyUpdatedSignal.subscribe {
            baseCurrencySubject.onNext(currencyManager.baseCurrency)
        })

        disposables.add(termsManager.termsAcceptedSignal.subscribe {
            termsAcceptedSubject.onNext(it)
        })

        disposables.add(pinComponent.pinSetFlowable.subscribe {
            pinSetSubject.onNext(pinComponent.isPinSet)
        })
    }

    fun stop() {
        disposables.clear()
    }

    fun setAppRelaunchingFromSettings() {
        localStorage.relaunchBySettingChange = true
    }

    fun getWalletConnectSupportState(): WalletConnectManager.SupportState {
        return walletConnectManager.getWalletConnectSupportState()
    }
}
