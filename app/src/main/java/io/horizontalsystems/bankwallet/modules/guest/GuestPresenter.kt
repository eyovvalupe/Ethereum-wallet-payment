package io.horizontalsystems.bankwallet.modules.guest

class GuestPresenter(private val interactor: GuestModule.IInteractor, private val router: GuestModule.IRouter) : GuestModule.IViewDelegate, GuestModule.IInteractorDelegate {

    var view: GuestModule.IView? = null

    override fun onViewDidLoad() {
        view?.setAppVersion(interactor.appVersion)
    }

    override fun createWalletDidClick() {
        interactor.createWallet()
    }

    override fun restoreWalletDidClick() {
        router.navigateToRestore()
    }

    // interactor delegate

    override fun didCreateWallet() {
        router.navigateToBackupRoutingToMain()
    }

    override fun didFailToCreateWallet() {
        view?.showError()
    }

}
