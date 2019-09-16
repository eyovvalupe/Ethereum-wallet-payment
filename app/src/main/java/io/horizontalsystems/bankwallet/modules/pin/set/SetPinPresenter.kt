package io.horizontalsystems.bankwallet.modules.pin.set

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.pin.*

class SetPinPresenter(
        override val view: PinView,
        val router: SetPinModule.Router,
        interactor: PinModule.Interactor) : ManagePinPresenter(view, interactor, pages = listOf(Page.ENTER, Page.CONFIRM)) {

    override fun viewDidLoad() {
        view.setTitle(R.string.SetPin_Title)

        val pinPages = mutableListOf<PinPage>()
        pages.forEach { page ->
            when (page) {
                Page.ENTER -> pinPages.add(PinPage(TopText.Description(R.string.SetPin_Info)))
                Page.CONFIRM -> pinPages.add(PinPage(TopText.Description(R.string.SetPin_ConfirmInfo)))
            }
        }
        view.addPages(pinPages)
    }

    override fun didSavePin() {
        router.dismissModuleWithSuccess()
    }

}
