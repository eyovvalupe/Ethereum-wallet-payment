package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal
import java.math.BigInteger

abstract class EthereumBaseAdapter(
        protected val ethereumKit: EthereumKit,
        val decimal: Int)
    : IAdapter, ISendEthereumAdapter, ITransactionsAdapter, IBalanceAdapter, IReceiveAdapter {

    override fun getReceiveAddressType(wallet: Wallet): String? = null

    override val debugInfo: String = ethereumKit.debugInfo()

    // ITransactionsAdapter

    override val confirmationsThreshold: Int = 12

    override val lastBlockInfo: LastBlockInfo?
        get() = ethereumKit.lastBlockHeight?.toInt()?.let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = ethereumKit.lastBlockHeightFlowable.map { Unit }

    // ISendEthereumAdapter

    override fun fee(gasPrice: Long, gasLimit: Long): BigDecimal {
        val value = BigDecimal(gasPrice) * BigDecimal(gasLimit)
        return convertToEther(value)
    }

    override fun send(amount: BigDecimal, address: String, gasPrice: Long, gasLimit: Long, actionId: String): Single<Unit> {
        return try {
            sendInternal(Address(address), scaleUp(amount), gasPrice, gasLimit, actionId)
        } catch (error: Throwable) {
            Single.error(error)
        }
    }

    override fun estimateGasLimit(toAddress: String?, value: BigDecimal, gasPrice: Long?): Single<Long> {
        return try {
            estimateGasLimitInternal(toAddress?.let { Address(it) }, scaleUp(value), gasPrice)
        } catch (error: Throwable) {
            Single.error(error)
        }
    }

    @Throws
    override fun validate(address: String) {
        AddressValidator.validate(address)
    }

    protected fun scaleDown(amount: BigDecimal, decimals: Int = decimal): BigDecimal {
        return amount.movePointLeft(decimals).stripTrailingZeros()
    }

    protected fun scaleUp(amount: BigDecimal, decimals: Int = decimal): BigInteger {
        return amount.movePointRight(decimals).toBigInteger()
    }

    protected fun convertToWei(amount: BigDecimal): BigInteger {
        return scaleUp(amount, EthereumAdapter.decimal)
    }

    private fun convertToEther(amount: BigDecimal): BigDecimal {
        return scaleDown(amount, EthereumAdapter.decimal)
    }
    // IReceiveAdapter

    override val receiveAddress: String
        get() = ethereumKit.receiveAddress.eip55

    protected fun balanceInBigDecimal(balance: BigInteger?, decimal: Int): BigDecimal {
        balance?.toBigDecimal()?.let {
            return scaleDown(it, decimal)
        } ?: return BigDecimal.ZERO
    }

    protected abstract fun sendInternal(address: Address, amount: BigInteger, gasPrice: Long, gasLimit: Long, actionId: String): Single<Unit>

    protected abstract fun estimateGasLimitInternal(toAddress: Address?, value: BigInteger, gasPrice: Long?): Single<Long>

}
