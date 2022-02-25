package io.horizontalsystems.bankwallet.modules.nft

import androidx.room.Embedded
import androidx.room.Entity
import java.math.BigDecimal

@Entity(primaryKeys = ["accountId", "tokenId"])
data class NftAssetRecord(
    val accountId: String,
    val collectionUid: String,
    val tokenId: String,
    val name: String,
    val imageUrl: String,
    val imagePreviewUrl: String,
    val description: String,

    @Embedded
    val lastSale: NftAssetLastSale?,

    @Embedded
    val contract: NftAssetContract,

    val ownedCount: Int = 1
)

data class NftAssetLastSale(
    val coinTypeId: String,
    val totalPrice: BigDecimal
)

data class NftAssetContract(
    val address: String,
    val type: String
)
