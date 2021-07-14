package io.horizontalsystems.bankwallet.modules.transactionInfo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoButtonType.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.adapters.TransactionInfoViewItem
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.disposables.CompositeDisposable
import java.util.*

class TransactionInfoViewModel(
    private val service: TransactionInfoService,
    private val factory: TransactionInfoViewItemFactory,
    private val transaction: TransactionRecord,
    private val wallet: Wallet,
    private val clearables: List<Clearable>
) : ViewModel() {

    val titleLiveData = MutableLiveData<TransactionInfoModule.TitleViewItem>()
    val showLockInfo = SingleLiveEvent<Date>()
    val showDoubleSpendInfo = SingleLiveEvent<Pair<String, String>>()
    val showShareLiveEvent = SingleLiveEvent<String>()
    val showTransactionLiveEvent = SingleLiveEvent<String>()
    val explorerButton = MutableLiveData<Pair<String, Boolean>>()

    val viewItemsLiveData = MutableLiveData<List<TransactionInfoViewItem?>>()

    private var explorerData: TransactionInfoModule.ExplorerData =
        getExplorerData(transaction.transactionHash, service.testMode, wallet.coin.type)

    private val disposables = CompositeDisposable()
    private var rates: Map<Coin, CurrencyValue> = mutableMapOf()

    init {
        service.ratesAsync
            .subscribeIO {
                updateRates(it)
            }
            .let {
                disposables.add(it)
            }

        service.getRates(coinsForRates, transaction.timestamp)

        updateViewItems()
    }

    private fun updateRates(rates: Map<Coin, CurrencyValue>) {
        this.rates = rates
        updateViewItems()
    }

    private fun updateViewItems() {
        val viewItems =
            factory.getMiddleSectionItems(transaction, rates, service.lastBlockInfo, explorerData)

        viewItemsLiveData.postValue(viewItems)
    }

    override fun onCleared() {
        clearables.forEach {
            it.clear()
        }
    }

    fun onClickLockInfo() {
//        transaction.lockInfo?.lockedUntil?.let {
//        showLockInfo.postValue(it)
//        }
    }

    fun onClickDoubleSpendInfo() {
//        transaction.conflictingTxHash?.let { conflictingTxHash ->
//        showDoubleSpendInfo.postValue(Pair(transaction.transactionHash, conflictingTxHash))
//        }
    }

    fun onRawTransaction() {
//        onCopy(service.getRaw(transaction.transactionHash))
    }

    fun onAdditionalButtonClick(buttonType: TransactionInfoButtonType) {
        when (buttonType) {
            is OpenExplorer -> buttonType.url?.let {
                showTransactionLiveEvent.postValue(it)
            }
            is RevokeApproval -> {
                TODO("Not yet implemented")
            }
            is Resend -> {
                TODO("Not yet implemented")
            }
        }
    }

    private val coinsForRates: List<Coin>
        get() {
            return when (val tx = transaction) {
                is EvmIncomingTransactionRecord -> listOf(tx.value.coin)
                is EvmOutgoingTransactionRecord -> listOf(tx.fee.coin, tx.value.coin)
                is SwapTransactionRecord -> listOf(
                    tx.fee,
                    tx.valueIn,
                    tx.valueOut
                ).mapNotNull { it?.coin }
                is ApproveTransactionRecord -> listOf(tx.fee.coin, tx.value.coin)
                is ContractCallTransactionRecord -> {
                    val internalEth: List<Coin> = tx.incomingInternalETHs.map { it.second.coin }
                    val incomingTokens: List<Coin> = tx.incomingEip20Events.map { it.second.coin }
                    val outgoingTokens: List<Coin> = tx.outgoingEip20Events.map { it.second.coin }
                    internalEth + incomingTokens + outgoingTokens
                }
                is BitcoinIncomingTransactionRecord -> listOf(tx.value.coin)
                is BitcoinOutgoingTransactionRecord -> listOf(
                    tx.fee,
                    tx.value
                ).mapNotNull { it?.coin }.distinctBy { it.type }
                else -> emptyList()
            }
        }

    private fun getExplorerData(
        hash: String,
        testMode: Boolean,
        coinType: CoinType
    ): TransactionInfoModule.ExplorerData {
        return when (coinType) {
            is CoinType.Bitcoin -> TransactionInfoModule.ExplorerData(
                "blockchair.com",
                if (testMode) null else "https://blockchair.com/bitcoin/transaction/$hash"
            )
            is CoinType.BitcoinCash -> TransactionInfoModule.ExplorerData(
                "btc.com",
                if (testMode) null else "https://bch.btc.com/$hash"
            )
            is CoinType.Litecoin -> TransactionInfoModule.ExplorerData(
                "blockchair.com",
                if (testMode) null else "https://blockchair.com/litecoin/transaction/$hash"
            )
            is CoinType.Dash -> TransactionInfoModule.ExplorerData(
                "dash.org",
                if (testMode) null else "https://insight.dash.org/insight/tx/$hash"
            )
            is CoinType.Ethereum,
            is CoinType.Erc20 -> {
                val domain = when (service.ethereumNetworkType(wallet.account)) {
                    EthereumKit.NetworkType.EthMainNet -> "etherscan.io"
                    EthereumKit.NetworkType.EthRopsten -> "ropsten.etherscan.io"
                    EthereumKit.NetworkType.EthKovan -> "kovan.etherscan.io"
                    EthereumKit.NetworkType.EthRinkeby -> "rinkeby.etherscan.io"
                    EthereumKit.NetworkType.EthGoerli -> "goerli.etherscan.io"
                    EthereumKit.NetworkType.BscMainNet -> throw IllegalArgumentException("")
                }
                TransactionInfoModule.ExplorerData("etherscan.io", "https://$domain/tx/0x$hash")
            }
            is CoinType.Bep2 -> TransactionInfoModule.ExplorerData(
                "binance.org",
                if (testMode) "https://testnet-explorer.binance.org/tx/$hash" else "https://explorer.binance.org/tx/$hash"
            )
            is CoinType.BinanceSmartChain,
            is CoinType.Bep20 -> TransactionInfoModule.ExplorerData(
                "bscscan.com",
                "https://bscscan.com/tx/$hash"
            )
            is CoinType.Zcash -> TransactionInfoModule.ExplorerData(
                "blockchair.com",
                if (testMode) null else "https://blockchair.com/zcash/transaction/$hash"
            )
            is CoinType.Unsupported -> throw IllegalArgumentException()
        }
    }

}
