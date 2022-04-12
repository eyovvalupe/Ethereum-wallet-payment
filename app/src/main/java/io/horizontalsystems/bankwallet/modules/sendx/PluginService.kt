package io.horizontalsystems.bankwallet.modules.sendx

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.hodler.HodlerData
import io.horizontalsystems.hodler.HodlerPlugin
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.marketkit.models.CoinType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PluginService(localStorage: ILocalStorage, coinType: CoinType) {
    val isLockTimeEnabled = localStorage.isLockTimeEnabled && coinType is CoinType.Bitcoin
    val lockTimeIntervals = listOf(null) + LockTimeInterval.values().toList()

    private var lockTimeInterval: LockTimeInterval? = null
    private var pluginData: Map<Byte, IPluginData>? = null

    private val _stateFlow = MutableStateFlow(
        State(
            lockTimeInterval = lockTimeInterval,
            pluginData = pluginData
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    fun setLockTimeInterval(lockTimeInterval: LockTimeInterval?) {
        this.lockTimeInterval = lockTimeInterval

        refreshPluginData()

        emitState()
    }

    private fun refreshPluginData() {
        pluginData = lockTimeInterval?.let {
            mapOf(HodlerPlugin.id to HodlerData(it))
        }
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                lockTimeInterval = lockTimeInterval,
                pluginData = pluginData
            )
        }
    }

    data class State(
        val lockTimeInterval: LockTimeInterval?,
        val pluginData: Map<Byte, IPluginData>?
    )

}
