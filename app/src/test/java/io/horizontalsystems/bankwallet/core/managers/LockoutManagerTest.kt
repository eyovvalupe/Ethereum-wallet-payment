package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ILockoutUntilDateFactory
import io.horizontalsystems.bankwallet.core.IUptimeProvider
import io.horizontalsystems.bankwallet.entities.LockoutState
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import java.util.*

class LockoutManagerTest {

    private val localStorage = Mockito.mock(ILocalStorage::class.java)
    private val uptimeProvider = Mockito.mock(IUptimeProvider::class.java)
    private val lockoutUntilDateFactory = Mockito.mock(ILockoutUntilDateFactory::class.java)
    private val lockoutManager = LockoutManager(localStorage, uptimeProvider, lockoutUntilDateFactory)

    @Test
    fun didFailUnlock_first() {
        val oldAttempts = null
        val newAttempts = 1
        whenever(localStorage.failedAttempts).thenReturn(oldAttempts)

        lockoutManager.didFailUnlock()
        verify(localStorage).failedAttempts = newAttempts
    }

    @Test
    fun didFailUnlock_third() {
        val oldAttempts = 2
        val newAttempts = 3
        whenever(localStorage.failedAttempts).thenReturn(oldAttempts)

        lockoutManager.didFailUnlock()
        verify(localStorage).failedAttempts = newAttempts
    }

    @Test
    fun currentStateUnlocked() {
        val attempts = null
        whenever(localStorage.failedAttempts).thenReturn(attempts)

        val state = LockoutState.Unlocked(attempts)

        Assert.assertEquals(lockoutManager.currentState, state)
    }

    @Test
    fun currentStateUnlocked_WithTwoAttempts() {
        val failedAttempts = 2
        val attemptsLeft = 3
        whenever(localStorage.failedAttempts).thenReturn(failedAttempts)

        val state = LockoutState.Unlocked(attemptsLeft)

        Assert.assertEquals(lockoutManager.currentState, state)
    }

    @Test
    fun currentStateUnlocked_NotLessThanOne() {
        val date = Date()
        val timestamp = date.time
        val unlockDate = Date()
        unlockDate.time = date.time + 5000

        val failedAttempts = 7

        whenever(localStorage.failedAttempts).thenReturn(failedAttempts)
        whenever(localStorage.lockoutUptime).thenReturn(timestamp)
        whenever(uptimeProvider.uptime).thenReturn(timestamp)
        whenever(lockoutUntilDateFactory.lockoutUntilDate(failedAttempts, timestamp, timestamp)).thenReturn(unlockDate)

        val state = LockoutState.Locked(unlockDate)

        Assert.assertEquals(lockoutManager.currentState, state)
    }

    @Test
    fun updateLockoutTimestamp() {
        val failedAttempts = 4
        val timestamp = Date().time

        whenever(localStorage.failedAttempts).thenReturn(failedAttempts)

        whenever(uptimeProvider.uptime).thenReturn(timestamp)

        lockoutManager.didFailUnlock()

        verify(localStorage).lockoutUptime = timestamp
    }

    @Test
    fun currentStateLocked() {
        val date = Date()
        val timestamp = date.time

        val unlockDate = Date()
        unlockDate.time = date.time + 5000

        val state = LockoutState.Locked(unlockDate)

        val failedAttempts = 5
        whenever(localStorage.failedAttempts).thenReturn(failedAttempts)
        whenever(localStorage.lockoutUptime).thenReturn(timestamp)
        whenever(uptimeProvider.uptime).thenReturn(timestamp)
        whenever(lockoutUntilDateFactory.lockoutUntilDate(failedAttempts, timestamp, timestamp)).thenReturn(unlockDate)

        Assert.assertEquals(lockoutManager.currentState, state)
    }


}
