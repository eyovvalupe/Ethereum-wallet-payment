package io.horizontalsystems.bankwallet.modules.nft

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import io.horizontalsystems.bankwallet.core.storage.AccountRecord

@Entity(
    primaryKeys = ["accountId", "uid"],
    foreignKeys = [ForeignKey(
        entity = AccountRecord::class,
        parentColumns = ["id"],
        childColumns = ["accountId"],
        onDelete = ForeignKey.CASCADE,
        deferred = true
    )
    ]
)
data class NftCollectionRecord(
    val accountId: String,
    val uid: String,
    val name: String,
    val imageUrl: String?,
    val totalSupply: Int,

    @Embedded(prefix = "averagePrice7d_")
    val averagePrice7d: NftAssetPrice?,

    @Embedded(prefix = "averagePrice30d_")
    val averagePrice30d: NftAssetPrice?,

    @Embedded(prefix = "floorPrice_")
    val floorPrice: NftAssetPrice?,

    @Embedded
    val links: CollectionLinks?,
)

data class CollectionLinks(
    val external_url: String?,
    val discord_url: String?,
    val twitter_username: String?
)


//val telegram_url: String?,
//val instagram_username: String?,
//val wiki_url: String?,