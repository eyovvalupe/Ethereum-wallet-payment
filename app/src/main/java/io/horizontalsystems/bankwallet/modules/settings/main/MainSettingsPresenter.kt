package io.horizontalsystems.bankwallet.modules.settings.main

import io.horizontalsystems.bankwallet.R

class MainSettingsPresenter(
        private val router: MainSettingsModule.IMainSettingsRouter,
        private val interactor: MainSettingsModule.IMainSettingsInteractor)
    : MainSettingsModule.IMainSettingsViewDelegate, MainSettingsModule.IMainSettingsInteractorDelegate {

    var view: MainSettingsModule.IMainSettingsView? = null

    override fun viewDidLoad() {
        view?.setTitle(R.string.Settings_Title)
        view?.setBaseCurrency(interactor.baseCurrency)
        view?.setLanguage(interactor.currentLanguage)
        view?.setLightMode(interactor.getLightMode())
        view?.setAppVersion(interactor.appVersion)
    }

    override fun didTapSecurity() {
        router.showSecuritySettings()
    }

    override fun didManageCoins() {
        router.showManageCoins()
    }

    override fun didTapBaseCurrency() {
        router.showBaseCurrencySettings()
    }

    override fun didTapLanguage() {
        router.showLanguageSettings()
    }

    override fun didSwitchLightMode(lightMode: Boolean) {
        interactor.setLightMode(lightMode)
    }

    override fun didTapAbout() {
        router.showAbout()
    }

    override fun didTapAppLink() {
        router.openAppLink()
    }

    override fun didUpdateBaseCurrency(baseCurrency: String) {
        view?.setBaseCurrency(baseCurrency)
    }

    override fun didUpdateLightMode() {
        router.reloadAppInterface()
    }

    override fun onClear() {
        interactor.clear()
    }

}
