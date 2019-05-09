package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ITransactionDataProviderManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.BitcoinForksProvider
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.EthereumForksProvider
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.Provider
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers.*
import io.reactivex.subjects.PublishSubject

class TransactionDataProviderManager(private val appConfig: IAppConfigProvider, private val localStorage: ILocalStorage)
    : ITransactionDataProviderManager {

    private val bitcoinProviders = when {
        appConfig.testMode -> listOf(HorsysBitcoinProvider(testMode = true))
        else -> listOf(
                HorsysBitcoinProvider(testMode = false),
                BlockChairBitcoinProvider(),
                BlockExplorerBitcoinProvider(),
                BtcComBitcoinProvider())
    }

    private val bitcoinCashProviders = when {
        appConfig.testMode -> listOf(HorsysBitcoinCashProvider(testMode = true))
        else -> listOf(
                HorsysBitcoinCashProvider(testMode = false),
                BlockChairBitcoinCashProvider(),
                BlockExplorerBitcoinCashProvider(),
                BtcComBitcoinCashProvider())
    }

    private val ethereumProviders = when {
        appConfig.testMode -> listOf(
//                    HorsysEthereumProvider(testMode = true),
                EtherscanEthereumProvider(testMode = true))
        else -> listOf(
                EtherscanEthereumProvider(testMode = false),
//                    HorsysEthereumProvider(testMode = false),
                BlockChairEthereumProvider())
    }

    private val dashProviders = when {
        appConfig.testMode -> listOf(HorsysDashProvider(true))
        else -> listOf(
                HorsysDashProvider(false),
                BlockChairDashProvider(),
                InsightDashProvider()
        )
    }

    override val baseProviderUpdatedSignal = PublishSubject.create<Unit>()

    override fun providers(coin: Coin): List<Provider> = when (coin.type) {
        is CoinType.Bitcoin -> bitcoinProviders
        is CoinType.BitcoinCash -> bitcoinCashProviders
        is CoinType.Ethereum, is CoinType.Erc20 -> ethereumProviders
        is CoinType.Dash -> dashProviders
    }

    override fun baseProvider(coin: Coin) = when (coin.type) {
        is CoinType.Bitcoin, is CoinType.BitcoinCash -> {
            bitcoin(localStorage.baseBitcoinProvider ?: bitcoinProviders[0].name)
        }
        is CoinType.Ethereum, is CoinType.Erc20 -> {
            ethereum(localStorage.baseEthereumProvider ?: ethereumProviders[0].name)
        }
        is CoinType.Dash -> {
            dash(localStorage.baseDashProvider ?: dashProviders[0].name)
        }
    }

    override fun setBaseProvider(name: String, coin: Coin) {
        when (coin.type) {
            is CoinType.Bitcoin, is CoinType.BitcoinCash -> {
                localStorage.baseBitcoinProvider = name
            }
            is CoinType.Ethereum, is CoinType.Erc20 -> {
                localStorage.baseEthereumProvider = name
            }
            is CoinType.Dash -> {
                localStorage.baseDashProvider = name
            }
        }

        baseProviderUpdatedSignal.onNext(Unit)
    }

    //
    // Providers
    //
    override fun bitcoin(name: String): BitcoinForksProvider {
        bitcoinProviders.let { list ->
            return list.find { it.name == name } ?: list[0]
        }
    }

    override fun bitcoinCash(name: String): BitcoinForksProvider {
        bitcoinCashProviders.let { list ->
            return list.find { it.name == name } ?: list[0]
        }
    }

    override fun dash(name: String): BitcoinForksProvider {
        dashProviders.let { list ->
            return list.find { it.name == name } ?: list[0]
        }
    }

    override fun ethereum(name: String): EthereumForksProvider {
        ethereumProviders.let { list ->
            return list.find { it.name == name } ?: list[0]
        }
    }
}
