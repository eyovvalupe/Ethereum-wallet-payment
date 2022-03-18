package io.horizontalsystems.bankwallet.core.factories

import android.content.Context
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IInitialSyncModeSettingsManager
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.adapters.*
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.managers.*
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.marketkit.models.CoinType

class AdapterFactory(
    private val context: Context,
    private val testMode: Boolean,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val evmSyncSourceManager: EvmSyncSourceManager,
    private val binanceKitManager: BinanceKitManager,
    private val backgroundManager: BackgroundManager,
    private val restoreSettingsManager: RestoreSettingsManager,
    private val coinManager: ICoinManager) {

    var initialSyncModeSettingsManager: IInitialSyncModeSettingsManager? = null

    private fun getEvmAdapter(wallet: Wallet): IAdapter? {
        val blockchain = evmBlockchainManager.getBlockchain(wallet.coinType) ?: return null
        val evmKitWrapper = evmBlockchainManager.getEvmKitManager(blockchain).getEvmKitWrapper(wallet.account, blockchain)

        return EvmAdapter(evmKitWrapper, coinManager)
    }

    private fun getEip20Adapter(wallet: Wallet, address: String): IAdapter? {
        val blockchain = evmBlockchainManager.getBlockchain(wallet.coinType) ?: return null
        val evmKitWrapper = evmBlockchainManager.getEvmKitManager(blockchain).getEvmKitWrapper(wallet.account, blockchain)
        val baseCoin = evmBlockchainManager.getBasePlatformCoin(blockchain) ?: return null

        return Eip20Adapter(context, evmKitWrapper, address, baseCoin, coinManager, wallet)
    }

    fun getAdapter(wallet: Wallet): IAdapter? {
        val syncMode = initialSyncModeSettingsManager?.setting(wallet.coinType, wallet.account.origin)?.syncMode ?: SyncMode.Fast

        return when (val coinType = wallet.coinType) {
            is CoinType.Zcash -> ZcashAdapter(context, wallet, restoreSettingsManager.settings(wallet.account, wallet.coinType), testMode)
            is CoinType.Bitcoin -> BitcoinAdapter(wallet, syncMode, testMode, backgroundManager)
            is CoinType.Litecoin -> LitecoinAdapter(wallet, syncMode, testMode, backgroundManager)
            is CoinType.BitcoinCash -> BitcoinCashAdapter(wallet, syncMode, testMode, backgroundManager)
            is CoinType.Dash -> DashAdapter(wallet, syncMode, testMode, backgroundManager)
            is CoinType.Bep2 -> {
                coinManager.getPlatformCoin(CoinType.Bep2("BNB"))?.let { feePlatformCoin ->
                    BinanceAdapter(binanceKitManager.binanceKit(wallet), coinType.symbol, feePlatformCoin, wallet, testMode)
                }
            }
            is CoinType.Ethereum, is CoinType.BinanceSmartChain, is CoinType.Polygon, is CoinType.EthereumOptimism, is CoinType.EthereumArbitrumOne -> getEvmAdapter(wallet)
            is CoinType.Erc20 -> getEip20Adapter(wallet, coinType.address)
            is CoinType.Bep20 -> getEip20Adapter(wallet, coinType.address)
            is CoinType.Mrc20 -> getEip20Adapter(wallet, coinType.address)
            is CoinType.OptimismErc20 -> getEip20Adapter(wallet, coinType.address)
            is CoinType.ArbitrumOneErc20 -> getEip20Adapter(wallet, coinType.address)
            is CoinType.Avalanche,
            is CoinType.Fantom,
            is CoinType.HarmonyShard0,
            is CoinType.HuobiToken,
            is CoinType.Iotex,
            is CoinType.Moonriver,
            is CoinType.OkexChain,
            is CoinType.Solana,
            is CoinType.Sora,
            is CoinType.Tomochain,
            is CoinType.Xdai,
            is CoinType.Unsupported -> null
        }
    }

    fun evmTransactionsAdapter(source: TransactionSource, blockchain: EvmBlockchain): ITransactionsAdapter? {
        val evmKitWrapper = evmBlockchainManager.getEvmKitManager(blockchain).getEvmKitWrapper(source.account, blockchain)
        val baseCoin = evmBlockchainManager.getBasePlatformCoin(blockchain) ?: return null
        val syncSource = evmSyncSourceManager.getSyncSource(source.account, blockchain)

        return EvmTransactionsAdapter(evmKitWrapper, baseCoin, coinManager, source, syncSource.transactionSource)
    }

    fun unlinkAdapter(wallet: Wallet) {
        when (val blockchain = wallet.transactionSource.blockchain) {
            is TransactionSource.Blockchain.Evm -> {
                val evmBlockchain = blockchain.evmBlockchain
                val evmKitManager = evmBlockchainManager.getEvmKitManager(evmBlockchain)
                evmKitManager.unlink(wallet.account)
            }
            is TransactionSource.Blockchain.Bep2 -> {
                binanceKitManager.unlink()
            }
            else -> Unit
        }
    }

    fun unlinkAdapter(transactionSource: TransactionSource) {
        when (val blockchain = transactionSource.blockchain) {
            is TransactionSource.Blockchain.Evm -> {
                val evmBlockchain = blockchain.evmBlockchain
                val evmKitManager = evmBlockchainManager.getEvmKitManager(evmBlockchain)
                evmKitManager.unlink(transactionSource.account)
            }
            is TransactionSource.Blockchain.Bep2 -> {
                binanceKitManager.unlink()
            }
            else -> Unit
        }
    }
}
