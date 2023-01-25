package io.horizontalsystems.bankwallet.core.managers

import android.net.Uri
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.bankwallet.core.storage.EvmSyncSourceStorage
import io.horizontalsystems.bankwallet.entities.EvmSyncSource
import io.horizontalsystems.bankwallet.entities.EvmSyncSourceRecord
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.net.URL

class EvmSyncSourceManager(
    private val appConfigProvider: AppConfigProvider,
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
    private val evmSyncSourceStorage: EvmSyncSourceStorage,
) {

    private val syncSourceSubject = PublishSubject.create<BlockchainType>()

    private val _syncSourcesUpdatedFlow =
        MutableSharedFlow<BlockchainType>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val syncSourcesUpdatedFlow = _syncSourcesUpdatedFlow.asSharedFlow()

    private fun defaultTransactionSource(blockchainType: BlockchainType): TransactionSource {
        return when (blockchainType) {
            BlockchainType.Ethereum -> TransactionSource.ethereumEtherscan(appConfigProvider.etherscanApiKey)
            BlockchainType.EthereumGoerli -> TransactionSource.goerliEtherscan(appConfigProvider.etherscanApiKey)
            BlockchainType.BinanceSmartChain -> TransactionSource.bscscan(appConfigProvider.bscscanApiKey)
            BlockchainType.Polygon -> TransactionSource.polygonscan(appConfigProvider.polygonscanApiKey)
            BlockchainType.Avalanche -> TransactionSource.snowtrace(appConfigProvider.snowtraceApiKey)
            BlockchainType.Optimism -> TransactionSource.optimisticEtherscan(appConfigProvider.optimisticEtherscanApiKey)
            BlockchainType.ArbitrumOne -> TransactionSource.arbiscan(appConfigProvider.arbiscanApiKey)
            BlockchainType.Gnosis -> TransactionSource.gnosis(appConfigProvider.gnosisscanApiKey)
            else -> throw Exception("Non-supported EVM blockchain")
        }
    }

    val syncSourceObservable: Observable<BlockchainType>
        get() = syncSourceSubject

    fun defaultSyncSources(blockchainType: BlockchainType): List<EvmSyncSource> {
        return when (val type = blockchainType) {
            BlockchainType.Ethereum -> listOf(
                evmSyncSource(
                    type,
                    "MainNet Websocket",
                    RpcSource.ethereumInfuraWebSocket(
                        appConfigProvider.infuraProjectId,
                        appConfigProvider.infuraProjectSecret
                    ),
                    defaultTransactionSource(type)
                ),
                evmSyncSource(
                    type,
                    "MainNet HTTP",
                    RpcSource.ethereumInfuraHttp(
                        appConfigProvider.infuraProjectId,
                        appConfigProvider.infuraProjectSecret
                    ),
                    defaultTransactionSource(type)
                ),
                evmSyncSource(
                    type,
                    "eth.llamarpc.com",
                    RpcSource.Http(listOf(URL("https://eth.llamarpc.com")), null),
                    defaultTransactionSource(type)
                )
            )
            BlockchainType.EthereumGoerli -> listOf(
                evmSyncSource(
                    type,
                    "Goerli HTTP",
                    RpcSource.goerliInfuraHttp(
                        appConfigProvider.infuraProjectId,
                        appConfigProvider.infuraProjectSecret
                    ),
                    defaultTransactionSource(type)
                )
            )
            BlockchainType.BinanceSmartChain -> listOf(
                evmSyncSource(
                    type,
                    "Default HTTP",
                    RpcSource.binanceSmartChainHttp(),
                    defaultTransactionSource(type)
                ),
                evmSyncSource(
                    type,
                    "BSC-RPC HTTP",
                    RpcSource.bscRpcHttp(),
                    defaultTransactionSource(type)
                ),
                evmSyncSource(
                    type,
                    "1rpc.io",
                    RpcSource.Http(listOf(URL("https://1rpc.io/bnb")), null),
                    defaultTransactionSource(type)
                )
            )
            BlockchainType.Polygon -> listOf(
                evmSyncSource(
                    type,
                    "Polygon-RPC HTTP",
                    RpcSource.polygonRpcHttp(),
                    defaultTransactionSource(type)
                ),
                evmSyncSource(
                    type,
                    "polygon.llamarpc.com",
                    RpcSource.Http(listOf(URL("https://polygon.llamarpc.com")), null),
                    defaultTransactionSource(type)
                )
            )
            BlockchainType.Avalanche -> listOf(
                evmSyncSource(
                    type,
                    "Avax.network",
                    RpcSource.avaxNetworkHttp(),
                    defaultTransactionSource(type)
                ),
                evmSyncSource(
                    type,
                    "avalanche-evm.publicnode.com",
                    RpcSource.Http(listOf(URL("https://avalanche-evm.publicnode.com")), null),
                    defaultTransactionSource(type)
                )
            )
            BlockchainType.Optimism -> listOf(
                evmSyncSource(
                    type,
                    "Optimism.io HTTP",
                    RpcSource.optimismRpcHttp(),
                    defaultTransactionSource(type)
                ),
                evmSyncSource(
                    type,
                    "endpoints.omniatech.io",
                    RpcSource.Http(
                        listOf(URL("https://endpoints.omniatech.io/v1/op/mainnet/public")),
                        null
                    ),
                    defaultTransactionSource(type)
                )
            )
            BlockchainType.ArbitrumOne -> listOf(
                evmSyncSource(
                    type,
                    "Arbitrum.io HTTP",
                    RpcSource.arbitrumOneRpcHttp(),
                    defaultTransactionSource(type)
                ),
                evmSyncSource(
                    type,
                    "1rpc.io",
                    RpcSource.Http(listOf(URL("https://1rpc.io/arb")), null),
                    defaultTransactionSource(type)
                )
            )
            BlockchainType.Gnosis -> listOf(
                evmSyncSource(
                    type,
                    "Gnosis.io HTTP",
                    RpcSource.gnosisRpcHttp(),
                    defaultTransactionSource(type)
                ),
                evmSyncSource(
                    type,
                    "rpc.ankr.com",
                    RpcSource.Http(listOf(URL("https://rpc.ankr.com/gnosis")), null),
                    defaultTransactionSource(type)
                )
            )
            else -> listOf()
        }
    }

    fun customSyncSources(blockchainType: BlockchainType): List<EvmSyncSource> {
        val records = evmSyncSourceStorage.evmSyncSources(blockchainType)
        return try {
            records.mapNotNull { record ->
                val uri = Uri.parse(record.url)
                val rpcSource = when (uri.scheme) {
                    "http",
                    "https" -> RpcSource.Http(listOf(URL(record.url)), record.auth)
                    "ws",
                    "wss" -> RpcSource.WebSocket(URL(record.url), record.auth)
                    else -> return@mapNotNull null
                }
                EvmSyncSource(
                    id = blockchainType.uid + "|" + record.url,
                    name = uri.host ?: "",
                    rpcSource = rpcSource,
                    transactionSource = defaultTransactionSource(blockchainType)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun evmSyncSource(
        blockchainType: BlockchainType,
        name: String,
        rpcSource: RpcSource,
        transactionSource: TransactionSource
    ) =
        EvmSyncSource(
            id = "${blockchainType.uid}|${name}|${transactionSource.name}|${
                rpcSource.urls.joinToString(separator = ",") { it.toString() }
            }",
            name = name,
            rpcSource = rpcSource,
            transactionSource = transactionSource
        )

    fun allSyncSources(blockchainType: BlockchainType): List<EvmSyncSource> =
        defaultSyncSources(blockchainType) + customSyncSources(blockchainType)

    fun getSyncSource(blockchainType: BlockchainType): EvmSyncSource {
        val syncSources = allSyncSources(blockchainType)

        val syncSourceUrl = blockchainSettingsStorage.evmSyncSourceUrl(blockchainType)
        val syncSource = syncSources.firstOrNull { it.url.toString() == syncSourceUrl }

        return syncSource ?: syncSources[0]
    }

    fun getHttpSyncSource(blockchainType: BlockchainType): EvmSyncSource? {
        val syncSources = allSyncSources(blockchainType)
        blockchainSettingsStorage.evmSyncSourceUrl(blockchainType)?.let { url ->
            syncSources.firstOrNull { it.url.toString() == url && it.isHttp }?.let { syncSource ->
                return syncSource
            }
        }

        return syncSources.firstOrNull { it.isHttp }
    }

    fun save(syncSource: EvmSyncSource, blockchainType: BlockchainType) {
        blockchainSettingsStorage.save(syncSource.url.toString(), blockchainType)
        syncSourceSubject.onNext(blockchainType)
    }

    fun saveSyncSource(blockchainType: BlockchainType, url: String, auth: String?) {
        val record = EvmSyncSourceRecord(
            blockchainTypeUid = blockchainType.uid,
            url = url,
            auth = auth
        )

        evmSyncSourceStorage.save(record)

        customSyncSources(blockchainType).firstOrNull { it.url.toString() == url }?.let {
            save(it, blockchainType)
        }

        _syncSourcesUpdatedFlow.tryEmit(blockchainType)
    }

    fun delete(syncSource: EvmSyncSource, blockchainType: BlockchainType) {
        val isCurrent = getSyncSource(blockchainType) == syncSource

        evmSyncSourceStorage.delete(blockchainType.uid, syncSource.url.toString())

        if (isCurrent) {
            syncSourceSubject.onNext(blockchainType)
        }

        _syncSourcesUpdatedFlow.tryEmit(blockchainType)
    }

}
