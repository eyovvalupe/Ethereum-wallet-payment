package io.horizontalsystems.bankwallet.modules.pin.unlock

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import io.horizontalsystems.bankwallet.entities.LockoutState
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.reset
import java.util.*

class UnlockPinPresenterTest {

    private val interactor = Mockito.mock(UnlockPinModule.IUnlockPinInteractor::class.java)
    private val router = Mockito.mock(UnlockPinModule.IUnlockPinRouter::class.java)
    private val view = Mockito.mock(PinModule.IPinView::class.java)
    private var presenter = UnlockPinPresenter(interactor, router)

    @Before
    fun setUp() {
        RxBaseTest.setup()
        presenter.view = view
    }

    @Test
    fun testAddPages() {
        presenter.viewDidLoad()
        verify(view).addPages(any())
    }

    @Test
    fun onUnlockEnter() {
        val pin ="000000"
        presenter.onEnter(pin, 0)

        verify(view).fillCircles(pin.length, 0)
        verify(interactor).unlock(pin)
    }

    @Test
    fun onUnlockEnter_notEnough() {
        val pin ="00000"
        presenter.onEnter(pin, 0)

        verify(interactor, never()).unlock(pin)
    }

    @Test
    fun onCancel() {
        presenter.onCancel()
        verify(router).dismiss(false)
    }

    @Test
    fun onDelete() {
        val pin ="12345"
        presenter.onEnter(pin, 0)
        verify(view).fillCircles(5, 0)
        reset(view)
        presenter.onDelete(0)
        verify(view).fillCircles(4, 0)
    }

    @Test
    fun didBiometricUnlock() {
        presenter.didBiometricUnlock()
        verify(router).dismiss(true)
    }

    @Test
    fun unlock() {
        presenter.unlock()
        verify(router).dismiss(true)
    }

    @Test
    fun showFingerprintInput() {
//        presenter.showFingerprintInput(cryptoObject)
//        verify(view).showFingerprintDialog(cryptoObject)
    }

    @Test
    fun wrongPinSubmitted() {
        val pin ="12345"
        presenter.onEnter(pin, 0)
        verify(view).fillCircles(5, 0)
        reset(view)
        presenter.wrongPinSubmitted()
        verify(view).showPinWrong(0)
    }

    @Test
    fun onBiometricUnlock() {
        presenter.onBiometricUnlock()
        verify(interactor).onUnlock()
    }




    @Test
    fun onViewDidLoad_UpdateLockoutState() {
        presenter.viewDidLoad()

        verify(interactor).updateLockoutState()
    }

    @Test
    fun updateLockoutState_Unlocked() {
        val attempts = null
        val pageIndex = 0
        val state = LockoutState.Unlocked(null)
        presenter.updateLockoutState(state)

        verify(view).showAttemptsLeft(attempts, pageIndex)
        verify(view, never()).showLockView(any())
    }

    @Test
    fun updateLockoutState_UnlockedWithFewAttempts() {
        val attempts = 3
        val pageIndex = 0
        val state = LockoutState.Unlocked(3)
        presenter.updateLockoutState(state)

        verify(view).showAttemptsLeft(attempts, pageIndex)
        verify(view, never()).showLockView(any())
    }

    @Test
    fun updateLockoutState_Locked() {
        val date = Date()
        val state = LockoutState.Locked(date)
        presenter.updateLockoutState(state)

        verify(view).showLockView(any())
        verify(view, never()).showAttemptsLeft(any(), any())
    }

    @Test
    fun onBackPressed() {
        presenter.onBackPressed()
        verify(router).closeApplication()
    }

}
