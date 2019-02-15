package io.horizontalsystems.bankwallet.modules.pin.unlock

import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.LockoutState
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.modules.pin.PinPage

class UnlockPinPresenter(
        private val interactor: UnlockPinModule.IUnlockPinInteractor,
        private val router: UnlockPinModule.IUnlockPinRouter) : PinModule.IPinViewDelegate, UnlockPinModule.IUnlockPinInteractorDelegate {

    private val unlockPageIndex = 0
    private var enteredPin = ""
    private var cryptoObject: FingerprintManagerCompat.CryptoObject? = null
    var view: PinModule.IPinView? = null

    override fun viewDidLoad() {
        interactor.cacheSecuredData()
        view?.hideToolbar()
        view?.addPages(listOf(PinPage(R.string.Unlock_Page_EnterYourPin)))

        interactor.updateLockoutState()
    }

    override fun onEnter(pin: String, pageIndex: Int) {
        if (enteredPin.length < PinModule.PIN_COUNT) {
            enteredPin += pin
            view?.fillCircles(enteredPin.length, pageIndex)

            if (enteredPin.length == PinModule.PIN_COUNT) {
                if (interactor.unlock(enteredPin)) {
                    router.dismiss(true)
                } else {
                    wrongPinSubmitted()
                }
                enteredPin = ""
            }
        }
    }

    override fun onCancel() {
        router.dismiss(false)
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

    override fun didBiometricUnlock() {
        router.dismiss(true)
    }

    override fun unlock() {
        router.dismiss(true)
    }

    override fun setCryptoObject(cryptoObject: FingerprintManagerCompat.CryptoObject) {
        this.cryptoObject = cryptoObject
        showBiometricUnlock()
    }

    override fun wrongPinSubmitted() {
        view?.showPinWrong(unlockPageIndex)
    }

    override fun showBiometricUnlock() {
        if (interactor.isBiometricOn()) {
            cryptoObject?.let { view?.showFingerprintDialog(it) }
        }
    }

    override fun onBiometricUnlock() {
        interactor.onUnlock()
    }

    override fun updateLockoutState(state: LockoutState) {
        when (state) {
            is LockoutState.Unlocked -> view?.showAttemptsLeft(state.attemptsLeft, unlockPageIndex)
            is LockoutState.Locked -> view?.showLockView(state.until)
        }
    }

    override fun onBackPressed() {
        router.closeApplication()
    }
}
