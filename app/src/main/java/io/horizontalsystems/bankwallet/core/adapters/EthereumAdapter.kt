package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.TransactionType
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionWithInternal
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal
import java.math.BigInteger

class EthereumAdapter(kit: EthereumKit) : EthereumBaseAdapter(kit, decimal) {

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

    override val state: AdapterState
        get() = when (val kitSyncState = ethereumKit.syncState) {
            is EthereumKit.SyncState.Synced -> AdapterState.Synced
            is EthereumKit.SyncState.NotSynced -> AdapterState.NotSynced(kitSyncState.error)
            is EthereumKit.SyncState.Syncing -> AdapterState.Syncing(50, null)
        }

    override fun sendInternal(address: Address, amount: BigInteger, gasPrice: Long, gasLimit: Long, logger: AppLogger): Single<Unit> {
        return ethereumKit.send(address, amount, byteArrayOf(),gasPrice, gasLimit)
                .doOnSubscribe {
                    logger.info("call ethereumKit.send")
                }
                .map { Unit }
    }

    override fun estimateGasLimitInternal(toAddress: Address?, value: BigInteger, gasPrice: Long?): Single<Long> {
        return ethereumKit.estimateGas(toAddress, value, gasPrice)
    }

    override val stateUpdatedFlowable: Flowable<Unit>
        get() = ethereumKit.syncStateFlowable.map { Unit }

    override val balance: BigDecimal
        get() = balanceInBigDecimal(ethereumKit.balance, decimal)

    override val minimumRequiredBalance: BigDecimal
        get() = BigDecimal.ZERO

    override val minimumSendAmount: BigDecimal
        get() = BigDecimal.ZERO

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = ethereumKit.balanceFlowable.map { Unit }

    // ITransactionsAdapter

    override fun getTransactions(from: TransactionRecord?, limit: Int): Single<List<TransactionRecord>> {
        return ethereumKit.transactions(from?.transactionHash?.hexStringToByteArray(), limit).map {
            it.map { tx -> transactionRecord(tx) }
        }
    }

    override val transactionRecordsFlowable: Flowable<List<TransactionRecord>>
        get() = ethereumKit.transactionsFlowable.map { it.map { tx -> transactionRecord(tx) } }


    private fun transactionRecord(transactionWithInternal: TransactionWithInternal): TransactionRecord {
        val transaction = transactionWithInternal.transaction
        var fromAddress = transaction.from
        var toAddress = transaction.to
        val myAddress = ethereumKit.receiveAddress
        val fromMine = fromAddress == myAddress
        val toMine = toAddress == myAddress
        val fee = transaction.gasUsed?.toBigDecimal()?.multiply(transaction.gasPrice.toBigDecimal())?.let { scaleDown(it) }

        var amount = if (fromMine) transaction.value.negate() else transaction.value
        transactionWithInternal.internalTransactions.forEach { internalTransaction ->
            var internalAmount = internalTransaction.value
            internalAmount = if (internalTransaction.from == myAddress) internalAmount.negate() else internalAmount
            amount += internalAmount
            fromAddress = internalTransaction.from
            toAddress = internalTransaction.to
        }
        val type = when {
            fromMine && toMine -> TransactionType.SentToSelf
            amount < BigInteger.ZERO -> TransactionType.Outgoing
            else -> TransactionType.Incoming
        }

        val txHashHex = transaction.hash.toHexString()
        return TransactionRecord(
                uid = txHashHex,
                transactionHash = txHashHex,
                transactionIndex = transaction.transactionIndex ?: 0,
                interTransactionIndex = 0,
                blockHeight = transaction.blockNumber,
                amount = scaleDown(amount.abs().toBigDecimal()),
                confirmationsThreshold = confirmationsThreshold,
                fee = fee,
                timestamp = transaction.timestamp,
                from = fromAddress.hex,
                to = toAddress.hex,
                type = type,
                failed = transaction.isError?.let { it != 0 } ?: false
        )
    }

    // ISendEthereumAdapter

    override val ethereumBalance: BigDecimal
        get() = balance

    override fun availableBalance(gasPrice: Long, gasLimit: Long): BigDecimal {
        return BigDecimal.ZERO.max(balance - fee(gasPrice, gasLimit))
    }


    companion object {
        const val decimal = 18

        fun clear(walletId: String, testMode: Boolean) {
            val networkType = if (testMode) EthereumKit.NetworkType.Ropsten else EthereumKit.NetworkType.MainNet
            EthereumKit.clear(App.instance, networkType, walletId)
        }
    }

}
