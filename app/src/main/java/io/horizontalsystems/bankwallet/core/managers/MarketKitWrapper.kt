package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.*
import io.reactivex.Observable

class MarketKitWrapper(
    context: Context,
    hsApiBaseUrl: String,
    hsApiKey: String,
    cryptoCompareApiKey: String? = null,
    defiYieldApiKey: String? = null
) {
    private val marketKit: MarketKit = MarketKit.getInstance(
        context = context,
        hsApiBaseUrl = hsApiBaseUrl,
        hsApiKey = hsApiKey,
        cryptoCompareApiKey = cryptoCompareApiKey,
        defiYieldApiKey = defiYieldApiKey
    )

    // Coins

    val fullCoinsUpdatedObservable: Observable<Unit>
        get() = marketKit.fullCoinsUpdatedObservable

    fun fullCoins(filter: String, limit: Int = 20) = marketKit.fullCoins(filter, limit)

    fun fullCoins(coinUids: List<String>) = marketKit.fullCoins(coinUids)

    fun token(query: TokenQuery) = marketKit.token(query)

    fun tokens(queries: List<TokenQuery>) = marketKit.tokens(queries)

    fun tokens(reference: String) = marketKit.tokens(reference)

    fun tokens(blockchainType: BlockchainType, filter: String, limit: Int = 20) = marketKit.tokens(blockchainType, filter, limit)

    fun blockchains(uids: List<String>) = marketKit.blockchains(uids)

    fun blockchain(uid: String) = marketKit.blockchain(uid)

    fun marketInfosSingle(top: Int, currencyCode: String, defi: Boolean = false) = marketKit.marketInfosSingle(top, currencyCode, defi)

    fun advancedMarketInfosSingle(top: Int = 250, currencyCode: String) = marketKit.advancedMarketInfosSingle(top, currencyCode)

    fun marketInfosSingle(coinUids: List<String>, currencyCode: String) = marketKit.marketInfosSingle(coinUids, currencyCode)

    fun marketInfosSingle(categoryUid: String, currencyCode: String) = marketKit.marketInfosSingle(categoryUid, currencyCode)

    fun marketInfoOverviewSingle(coinUid: String, currencyCode: String, language: String) =
        marketKit.marketInfoOverviewSingle(coinUid, currencyCode, language)

    fun marketInfoDetailsSingle(coinUid: String, currencyCode: String) = marketKit.marketInfoDetailsSingle(coinUid, currencyCode)

    fun marketInfoTvlSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod) =
        marketKit.marketInfoTvlSingle(coinUid, currencyCode, timePeriod)

    fun marketInfoGlobalTvlSingle(chain: String, currencyCode: String, timePeriod: HsTimePeriod) =
        marketKit.marketInfoTvlSingle(chain, currencyCode, timePeriod)

    fun defiMarketInfosSingle(currencyCode: String) = marketKit.defiMarketInfosSingle(currencyCode)

    // Categories

    fun coinCategoriesSingle(currencyCode: String) = marketKit.coinCategoriesSingle(currencyCode)

    fun coinCategoryMarketPointsSingle(categoryUid: String, interval: HsTimePeriod, currencyCode: String) =
        marketKit.coinCategoryMarketPointsSingle(categoryUid, interval, currencyCode)

    fun sync() = marketKit.sync()

    // Coin Prices

    fun refreshCoinPrices(currencyCode: String) = marketKit.refreshCoinPrices(currencyCode)

    fun coinPrice(coinUid: String, currencyCode: String) = marketKit.coinPrice(coinUid, currencyCode)

    fun coinPriceMap(coinUids: List<String>, currencyCode: String) = marketKit.coinPriceMap(coinUids, currencyCode)

    fun coinPriceObservable(coinUid: String, currencyCode: String) = marketKit.coinPriceObservable(coinUid, currencyCode)

    fun coinPriceMapObservable(coinUids: List<String>, currencyCode: String) = marketKit.coinPriceMapObservable(coinUids, currencyCode)

    // Coin Historical Price

    fun coinHistoricalPriceSingle(coinUid: String, currencyCode: String, timestamp: Long) =
        marketKit.coinHistoricalPriceSingle(coinUid, currencyCode, timestamp)

    fun coinHistoricalPrice(coinUid: String, currencyCode: String, timestamp: Long) = marketKit.coinHistoricalPrice(coinUid, currencyCode, timestamp)

    // Posts

    fun postsSingle() = marketKit.postsSingle()

    // Market Tickers

    fun marketTickersSingle(coinUid: String) = marketKit.marketTickersSingle(coinUid)

    // Details

    fun topHoldersSingle(coinUid: String) = marketKit.topHoldersSingle(coinUid)

    fun treasuriesSingle(coinUid: String, currencyCode: String) = marketKit.treasuriesSingle(coinUid, currencyCode)

    fun investmentsSingle(coinUid: String) = marketKit.investmentsSingle(coinUid)

    fun coinReportsSingle(coinUid: String) = marketKit.coinReportsSingle(coinUid)

    fun auditReportsSingle(addresses: List<String>) = marketKit.auditReportsSingle(addresses)

    // Pro Details

    fun dexLiquiditySingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod, sessionKey: String?) =
        marketKit.dexLiquiditySingle(coinUid, currencyCode, timePeriod, sessionKey)

    fun dexVolumesSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod, sessionKey: String?) =
        marketKit.dexVolumesSingle(coinUid, currencyCode, timePeriod, sessionKey)

    fun transactionDataSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod, platform: String?, sessionKey: String?) =
        marketKit.transactionDataSingle(coinUid, currencyCode, timePeriod, platform, sessionKey)

    fun activeAddressesSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod, sessionKey: String?) =
        marketKit.activeAddressesSingle(coinUid, currencyCode, timePeriod, sessionKey)

    // Overview

    fun marketOverviewSingle(currencyCode: String) = marketKit.marketOverviewSingle(currencyCode)

    fun topMoversSingle(currencyCode: String) = marketKit.topMoversSingle(currencyCode)

    // Chart Info

    fun chartInfo(coinUid: String, currencyCode: String, interval: HsTimePeriod) = marketKit.chartInfo(coinUid, currencyCode, interval)

    fun chartInfoSingle(coinUid: String, currencyCode: String, interval: HsTimePeriod) = marketKit.chartInfoSingle(coinUid, currencyCode, interval)

    fun getChartInfoAsync(coinUid: String, currencyCode: String, interval: HsTimePeriod) =
        marketKit.getChartInfoAsync(coinUid, currencyCode, interval)

    // Global Market Info

    fun globalMarketPointsSingle(currencyCode: String, timePeriod: HsTimePeriod) =
        marketKit.globalMarketPointsSingle(currencyCode, timePeriod)

    fun topPlatformsSingle(currencyCode: String) =
        marketKit.topPlatformsSingle(currencyCode)

    fun topPlatformMarketCapPointsSingle(chain: String, timePeriod: HsTimePeriod, currencyCode: String) =
        marketKit.topPlatformMarketCapPointsSingle(chain, timePeriod, currencyCode)

    fun topPlatformCoinListSingle(chain: String, currencyCode: String) =
        marketKit.topPlatformCoinListSingle(chain, currencyCode)

    // NFT

    suspend fun nftAssetCollection(address: String): NftAssetCollection =
        marketKit.nftAssetCollection(address)

    suspend fun nftCollection(uid: String): NftCollection =
        marketKit.nftCollection(uid)

    suspend fun nftCollections(): List<NftCollection> =
        marketKit.nftCollections()

    suspend fun nftAsset(contractAddress: String, tokenId: String): NftAsset =
        marketKit.nftAsset(contractAddress, tokenId)

    suspend fun nftAssets(collectionUid: String, cursor: String? = null): PagedNftAssets =
        marketKit.nftAssets(collectionUid, cursor)

    suspend fun nftCollectionEvents(collectionUid: String, eventType: NftEvent.EventType?, cursor: String? = null): PagedNftEvents =
        marketKit.nftCollectionEvents(collectionUid, eventType, cursor)

    suspend fun nftAssetEvents(contractAddress: String, tokenId: String, eventType: NftEvent.EventType?, cursor: String? = null): PagedNftEvents =
        marketKit.nftAssetEvents(contractAddress, tokenId, eventType, cursor)

}
