package io.horizontalsystems.bankwallet.core.storage

import android.arch.persistence.room.*
import io.horizontalsystems.bankwallet.entities.StorableCoin
import io.reactivex.Flowable

@Dao
interface StorableCoinsDao {

    @Query("SELECT * FROM StorableCoin ORDER BY `coinTitle` ASC")
    fun getAllCoins(): Flowable<List<StorableCoin>>

    @Query("SELECT * FROM StorableCoin WHERE enabled = 1 ORDER BY `order` ASC")
    fun getEnabledCoin(): Flowable<List<StorableCoin>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(storableCoin: StorableCoin)

    @Query("DELETE FROM StorableCoin WHERE coinCode = :coinCode")
    fun deleteByCode(coinCode: String)

    @Query("DELETE FROM StorableCoin")
    fun deleteAll()

    @Query("UPDATE StorableCoin SET enabled = 0, `order` = null")
    fun resetCoinsState()

    @Transaction
    fun setEnabledCoins(coins: List<StorableCoin>) {
        resetCoinsState()
        coins.forEach { insert(it) }
    }

    @Transaction
    fun insertCoins(coins: List<StorableCoin>) {
        coins.forEach { insert(it) }
    }

}
