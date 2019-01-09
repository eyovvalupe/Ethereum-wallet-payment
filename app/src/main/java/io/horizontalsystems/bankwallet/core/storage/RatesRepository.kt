package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import java.util.concurrent.Executors

class RatesRepository(private val appDatabase: AppDatabase) : IRateStorage {

    private val executor = Executors.newSingleThreadExecutor()

    override fun rateObservable(coinCode: CoinCode, currencyCode: String): Flowable<Rate> {
        return appDatabase.ratesDao().getRate(coinCode, currencyCode)
    }

    override fun save(rate: Rate) {
        executor.execute {
            appDatabase.ratesDao().insert(rate)
        }
    }

    override fun getAll(): Flowable<List<Rate>> {
        return appDatabase.ratesDao().getAll()
    }

    override fun deleteAll() {
        executor.execute {
            appDatabase.ratesDao().deleteAll()
        }
    }

}
