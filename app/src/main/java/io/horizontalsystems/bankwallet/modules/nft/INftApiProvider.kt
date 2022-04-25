package io.horizontalsystems.bankwallet.modules.nft

import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.hsnft.AssetOrder
import io.horizontalsystems.bankwallet.modules.hsnft.CollectionStats
import java.math.BigDecimal
import java.math.RoundingMode

interface INftApiProvider {
    suspend fun getCollectionRecords(address: Address, account: Account): List<NftCollectionRecord>
    suspend fun getAssetRecords(address: Address, account: Account): List<NftAssetRecord>
    suspend fun collectionStats(collectionUid: String): CollectionStats
    suspend fun assetOrders(contractAddress: String, tokenId: String): List<AssetOrder>

    suspend fun topCollections(count: Int): List<TopNftCollection>
    suspend fun collection(uid: String): NftCollection
}

data class TopNftCollection(
    val uid: String,
    val name: String,
    val imageUrl: String?,
    val floorPrice: BigDecimal?,
    val totalVolume: BigDecimal,
    val oneDayVolume: BigDecimal,
    val oneDayVolumeDiff: BigDecimal,
    val sevenDayVolume: BigDecimal,
    val sevenDayVolumeDiff: BigDecimal,
    val thirtyDayVolume: BigDecimal,
    val thirtyDayVolumeDiff: BigDecimal
)

data class NftCollection(
    val uid: String,
    val name: String,
    val imageUrl: String?,
    val description: String?,
    val ownersCount: Int,
    val totalSupply: Int,
    val oneDayVolume: BigDecimal,
    val oneDaySales: Int,
    val oneDayAveragePrice: BigDecimal,
    val averagePrice: BigDecimal,
    val floorPrice: BigDecimal?,
    val chartPoints: List<ChartPoint>,
    val links: Links?,
    val contracts: List<Contract>
) {
    val oneDayVolumeChange: BigDecimal?
        get() {
            val firstValue = chartPoints.firstOrNull()?.oneDayVolume
            val lastValue = chartPoints.lastOrNull()?.oneDayVolume

            return diff(firstValue, lastValue)
        }

    val oneDayAveragePriceChange: BigDecimal?
        get() {
            val firstValue = chartPoints.firstOrNull()?.averagePrice
            val lastValue = chartPoints.lastOrNull()?.averagePrice

            return diff(firstValue, lastValue)
        }

    val oneDaySalesChange: BigDecimal?
        get() {
            val firstValue = chartPoints.firstOrNull()?.oneDaySales?.toBigDecimal()
            val lastValue = chartPoints.lastOrNull()?.oneDaySales?.toBigDecimal()

            return diff(firstValue, lastValue)
        }

    val oneDayFloorPriceChange: BigDecimal?
        get() {
            val firstValue = chartPoints.firstOrNull()?.floorPrice
            val lastValue = chartPoints.lastOrNull()?.floorPrice

            return diff(firstValue, lastValue)
        }

    private fun diff(firstValue: BigDecimal?, lastValue: BigDecimal?) =
        if (firstValue != null && lastValue != null) {
            lastValue.subtract(firstValue).divide(firstValue, 4, RoundingMode.HALF_UP)
        } else {
            null
        }

    data class ChartPoint(
        val timestamp: Long,
        val oneDayVolume: BigDecimal,
        val averagePrice: BigDecimal,
        val floorPrice: BigDecimal?,
        val oneDaySales: Int
    )

    data class Links(
        val externalUrl: String?,
        val discordUrl: String?,
        val telegramUrl: String?,
        val twitterUsername: String?,
        val instagramUsername: String?,
        val wikiUrl: String?,
    )

    data class Contract(
        val address: String,
        val type: String,
    )
}
