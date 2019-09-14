package io.horizontalsystems.bankwallet.modules.pin

import io.horizontalsystems.bankwallet.R

open class ManagePinPresenter(
        private val interactor: PinModule.IPinInteractor,
        val pages: List<Page>) : PinModule.IPinViewDelegate, PinModule.IPinInteractorDelegate {

    enum class Page { UNLOCK, ENTER, CONFIRM }

    var view: PinModule.IPinView? = null
    private var enteredPin = ""
    private var isShowingPinMismatchError = false

    override fun viewDidLoad() {
    }

    override fun onEnter(pin: String, pageIndex: Int) {
        if (enteredPin.length < PinModule.PIN_COUNT) {
            enteredPin += pin
            removeErrorMessage(pageIndex)
            view?.fillCircles(enteredPin.length, pageIndex)

            if (enteredPin.length == PinModule.PIN_COUNT) {
                navigateToPage(pageIndex, enteredPin)
            }
        }
    }

    override fun resetPin() {
        enteredPin = ""
    }

    override fun onDelete(pageIndex: Int) {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.substring(0, enteredPin.length - 1)
            view?.fillCircles(enteredPin.length, pageIndex)
        }
    }

    override fun didSavePin() {
    }

    override fun didFailToSavePin() {
        showEnterPage()
        view?.showError(R.string.SetPin_ErrorFailedToSavePin)
    }

    private fun removeErrorMessage(pageIndex: Int) {
        if (isShowingPinMismatchError && pages[pageIndex] == Page.ENTER && enteredPin.isNotEmpty()) {
            view?.updateTopTextForPage(TopText.Description(R.string.EditPin_NewPinInfo), pageIndex)
            isShowingPinMismatchError = false
        }
    }

    private fun navigateToPage(pageIndex: Int, pin: String) {
        when (pages[pageIndex]) {
            Page.UNLOCK -> onEnterFromUnlock(pin)
            Page.ENTER -> onEnterFromEnterPage(pin)
            Page.CONFIRM -> onEnterFromConfirmPage(pin)
        }
    }

    private fun show(page: Page) {
        val pageIndex = pages.indexOfFirst { it == page }
        if (pageIndex >= 0) {
            view?.showPage(pageIndex)
        }
    }

    private fun showEnterPage() {
        interactor.set(null)
        show(Page.ENTER)
    }

    private fun onEnterFromUnlock(pin: String) {
        if (interactor.unlock(pin)) {
            show(Page.ENTER)
        } else {
            val pageUnlockIndex = pages.indexOfFirst { it == Page.UNLOCK }
            if (pageUnlockIndex >= 0) {
                view?.showPinWrong(pageUnlockIndex)
            }
        }
    }

    private fun onEnterFromEnterPage(pin: String) {
        interactor.set(pin)
        show(Page.CONFIRM)
    }

    private fun onEnterFromConfirmPage(pin: String) {
        if (interactor.validate(pin)) {
            interactor.save(pin)
        } else {
            showEnterPage()
            isShowingPinMismatchError = true
            pages.indexOfFirst { it == Page.ENTER }.let { pageIndex ->
                if (pageIndex >= 0) {
                    view?.updateTopTextForPage(TopText.SmallError(R.string.SetPin_ErrorPinsDontMatch), pageIndex)
                }
            }
        }
    }
}
