package io.horizontalsystems.bankwallet.modules.keystore

class KeyStorePresenter(private val interactor: KeyStoreModule.IInteractor,
                        private val router: KeyStoreModule.IRouter,
                        private val mode: KeyStoreModule.ModeType) : KeyStoreModule.IViewDelegate, KeyStoreModule.IInteractorDelegate {

    var view: KeyStoreModule.IView? = null

    override fun viewDidLoad() {
        when (mode) {
            KeyStoreModule.ModeType.NoSystemLock -> {
                interactor.resetApp()
                view?.showNoSystemLockWarning()
            }
            KeyStoreModule.ModeType.InvalidKey -> {
                interactor.resetApp()
                view?.showInvalidKeyWarning()
            }
            KeyStoreModule.ModeType.UserAuthentication -> view?.promptUserAuthentication()
        }
    }

    override fun onResume() {
        if (!interactor.isSystemLockOff &&
                !interactor.isKeyInvalidated &&
                !interactor.isUserNotAuthenticated) {
            interactor.removeKey()
            router.openLaunchModule()
        }
    }

    override fun onCloseInvalidKeyWarning() {
        interactor.removeKey()
        router.openLaunchModule()
    }

    override fun onAuthenticationCanceled() {
        router.closeApplication()
    }

    override fun onAuthenticationSuccess() {
        router.openLaunchModule()
    }

}
