package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SubscriptionManager(private val localStorage: ILocalStorage) {

    val showPremiumFeatureWarningFlow = MutableSharedFlow<Unit>()

    var authToken: String? = ""

    private val _authTokenFlow = MutableStateFlow(authToken)
    val authTokenFlow: StateFlow<String?> = _authTokenFlow

    fun hasSubscription(): Boolean {
        return true
    }

    suspend fun showPremiumFeatureWarning() {
        showPremiumFeatureWarningFlow.emit(Unit)
    }

}