package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.ISendSolanaAdapter
import io.horizontalsystems.bankwallet.core.managers.SolanaKitWrapper
import io.horizontalsystems.solanakit.SolanaKit
import io.horizontalsystems.solanakit.models.FullTransaction
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.rxSingle
import java.math.BigDecimal
import java.math.BigInteger

class SolanaAdapter(kitWrapper: SolanaKitWrapper) : BaseSolanaAdapter(kitWrapper, decimal), ISendSolanaAdapter {

    // IAdapter

    override fun start() {
        // started via EthereumKitManager
    }

    override fun stop() {
        // stopped via EthereumKitManager
    }

    override fun refresh() {
        // refreshed via EthereumKitManager
    }

    // IBalanceAdapter

    override val balanceState: AdapterState
        get() = convertToAdapterState(solanaKit.syncState)

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = solanaKit.balanceSyncStateFlow.map {}.asFlowable()

    override val balanceData: BalanceData
        get() = BalanceData(balanceInBigDecimal(solanaKit.balance, decimal))

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = solanaKit.balanceFlow.map {}.asFlowable()

    // ISendSolanaAdapter
    override fun send(amount: BigInteger, to: String): Single<FullTransaction> {
        if (signer == null) return Single.error(Exception())

        return rxSingle(Dispatchers.IO) {
            solanaKit.sendSol(to, amount.toBigDecimal().movePointLeft(decimal).toLong(), signer)
        }
    }

    private fun convertToAdapterState(syncState: SolanaKit.SyncState): AdapterState =
        when (syncState) {
            is SolanaKit.SyncState.Synced -> AdapterState.Synced
            is SolanaKit.SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
            is SolanaKit.SyncState.Syncing -> AdapterState.Syncing()
        }

    private fun scaleDown(amount: BigDecimal, decimals: Int = decimal): BigDecimal {
        return amount.movePointLeft(decimals).stripTrailingZeros()
    }

    private fun scaleUp(amount: BigDecimal, decimals: Int = decimal): BigInteger {
        return amount.movePointRight(decimals).toBigInteger()
    }

    private fun balanceInBigDecimal(balance: Long?, decimal: Int): BigDecimal {
        balance?.toBigDecimal()?.let {
            return scaleDown(it, decimal)
        } ?: return BigDecimal.ZERO
    }

    companion object {
        const val decimal = 9

        fun clear(walletId: String, testMode: Boolean) {
            SolanaKit.clear(App.instance, walletId)
        }
    }

}
