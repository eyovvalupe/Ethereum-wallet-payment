package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.binancechainkit.models.TransactionInfo
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal

class BinanceAdapter(override val wallet: Wallet, private val binanceKit: BinanceChainKit, private val symbol: String)
    : IAdapter {

    private val asset = binanceKit.register(symbol)

    override val feeCoinCode: String? = "BNB"
    override val decimal: Int = 8

    override val confirmationsThreshold: Int
        get() = 6

    override fun start() {
        // handled by BinanceKitManager
    }

    override fun stop() {
        // handled by BinanceKitManager
    }

    override fun refresh() {
        // handled by BinanceKitManager
    }

    override val lastBlockHeight: Int?
        get() = binanceKit.latestBlock?.height

    override val lastBlockHeightUpdatedFlowable: Flowable<Unit>
        get() = binanceKit.latestBlockFlowable.map { Unit }

    override val state: AdapterState
        get() = when (binanceKit.syncState) {
            BinanceChainKit.SyncState.Synced -> AdapterState.Synced
            BinanceChainKit.SyncState.NotSynced -> AdapterState.NotSynced
            BinanceChainKit.SyncState.Syncing -> AdapterState.Syncing(50, null)
        }

    override val stateUpdatedFlowable: Flowable<Unit>
        get() = binanceKit.syncStateFlowable.map { Unit }

    override val balance: BigDecimal
        get() = asset.balance

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = asset.balanceFlowable.map { Unit }

    override fun getTransactions(from: Pair<String, Int>?, limit: Int): Single<List<TransactionRecord>> {
        return binanceKit.transactions(asset, from?.first, limit).map { list ->
            list.map { transactionRecord(it) }
        }
    }

    override val transactionRecordsFlowable: Flowable<List<TransactionRecord>>
        get() = asset.transactionsFlowable.map { it.map { tx -> transactionRecord(tx) } }

    override fun send(params: Map<SendModule.AdapterFields, Any?>): Single<Unit> {
        val coinValue = params[SendModule.AdapterFields.CoinValue] as? CoinValue
                ?: throw WrongParameters()
        val address = params[SendModule.AdapterFields.Address] as? String
                ?: throw WrongParameters()
        val memo = params[SendModule.AdapterFields.Memo] as? String ?: ""

        return binanceKit.send(symbol, address, coinValue.value, memo).map { Unit }
    }

    override fun availableBalance(params: Map<SendModule.AdapterFields, Any?>): BigDecimal {
        val available = asset.balance - transferFee
        return if (available < BigDecimal.ZERO) BigDecimal.ZERO else available
    }

    override fun fee(params: Map<SendModule.AdapterFields, Any?>): BigDecimal {
        return transferFee
    }

    override fun getFeeRate(feeRatePriority: FeeRatePriority): Long {
        return 0L
    }

    override fun validate(address: String) {
        binanceKit.validateAddress(address)
    }

    override fun validate(params: Map<SendModule.AdapterFields, Any?>): List<SendStateError> {
        val amount = params[SendModule.AdapterFields.CoinAmountInBigDecimal] as? BigDecimal
                ?: throw WrongParameters()

        val errors = mutableListOf<SendStateError>()
        val availableBalance = availableBalance(params)
        if (availableBalance < amount) {
            errors.add(SendStateError.InsufficientAmount(availableBalance))
        }

        return errors
    }

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        return PaymentRequestAddress(address, amount = null)
    }

    override val receiveAddress: String
        get() = binanceKit.receiveAddress()

    override val debugInfo: String
        get() = ""

    private fun transactionRecord(transaction: TransactionInfo): TransactionRecord {
        val from = TransactionAddress(
                transaction.from,
                transaction.from == binanceKit.receiveAddress()
        )

        val to = TransactionAddress(
                transaction.to,
                transaction.to == binanceKit.receiveAddress()
        )

        var amount = BigDecimal.ZERO
        if (from.mine) {
            amount -= transaction.amount.toBigDecimal()
            amount -= transaction.fee.toBigDecimal()
        }
        if (to.mine) {
            amount += transaction.amount.toBigDecimal()
        }

        return TransactionRecord(
                transactionHash = transaction.hash,
                transactionIndex = 0,
                interTransactionIndex = 0,
                blockHeight = transaction.blockNumber.toLong(),
                amount = amount,
                timestamp = transaction.date.time / 1000,
                from = listOf(from),
                to = listOf(to)
        )
    }

    companion object {
        val transferFee = BigDecimal(0.000375)
    }
}
