package io.horizontalsystems.bankwallet.core.managers

import android.security.keystore.UserNotAuthenticatedException
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IPinManager
import io.horizontalsystems.bankwallet.core.ISecuredStorage
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.AuthData
import io.reactivex.subjects.PublishSubject

class AuthManager(private val secureStorage: ISecuredStorage, private val localStorage: ILocalStorage) {

    var walletManager: IWalletManager? = null
    var pinManager: IPinManager? = null
    var transactionManager: TransactionManager? = null

    var authData: AuthData? = null
    var authDataSignal = PublishSubject.create<Unit>()

    var isLoggedIn: Boolean = false
        get() = !secureStorage.noAuthData()


    @Throws(UserNotAuthenticatedException::class)
    fun safeLoad() {
        authData = secureStorage.authData
        authDataSignal.onNext(Unit)
    }

    @Throws(UserNotAuthenticatedException::class)
    fun login(words: List<String>) {
        AuthData(words).let {
            secureStorage.saveAuthData(it)
            authData = it
            walletManager?.initWallets()
        }
    }

    fun logout() {
        walletManager?.clearWallets()
        pinManager?.clear()
        transactionManager?.clear()
        localStorage.clearAll()

//        todo: clear authData from secureStorage
//        secureStorage.clearAuthData()
        authData = null

    }

}
