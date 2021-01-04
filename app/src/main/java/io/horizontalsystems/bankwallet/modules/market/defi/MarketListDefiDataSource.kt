package io.horizontalsystems.bankwallet.modules.market.defi

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.modules.market.top.IMarketListDataSource
import io.horizontalsystems.xrateskit.entities.TimePeriod
import io.horizontalsystems.xrateskit.entities.TopMarket
import io.reactivex.Observable
import io.reactivex.Single

class MarketListDefiDataSource(private val xRateManager: IRateManager) : IMarketListDataSource() {

    override val dataUpdatedAsync: Observable<Unit> = Observable.empty()

    override fun doGetListAsync(currencyCode: String, fetchDiffPeriod: TimePeriod): Single<List<TopMarket>> {
        return xRateManager.getTopDefiMarketList(currencyCode, fetchDiffPeriod)
    }

}
