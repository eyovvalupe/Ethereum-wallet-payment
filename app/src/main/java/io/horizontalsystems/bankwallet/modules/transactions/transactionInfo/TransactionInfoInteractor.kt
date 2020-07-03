package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.providers.FeeCoinProvider
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.core.ICurrencyManager

class TransactionInfoInteractor(
        private var clipboardManager: IClipboardManager,
        private val adapter: ITransactionsAdapter,
        private val xRateManager: IRateManager,
        private val currencyManager: ICurrencyManager,
        private val feeCoinProvider: FeeCoinProvider
) : TransactionInfoModule.Interactor {
    var delegate: TransactionInfoModule.InteractorDelegate? = null

    override val lastBlockInfo: LastBlockInfo?
        get() = adapter.lastBlockInfo

    override val threshold: Int
        get() = adapter.confirmationsThreshold

    override fun getRate(code: String, timestamp: Long): CurrencyValue? {
        val baseCurrency = currencyManager.baseCurrency

        return xRateManager.historicalRateCached(code, baseCurrency.code, timestamp)?.let {
            CurrencyValue(baseCurrency, it)
        }
    }

    override fun copyToClipboard(value: String) {
        clipboardManager.copyText(value)
    }

    override fun feeCoin(coin: Coin): Coin? {
        return feeCoinProvider.feeCoinData(coin)?.first
    }

    override fun getRaw(transactionHash: String): String? {
        return adapter.getRawTransaction(transactionHash)
    }
}
