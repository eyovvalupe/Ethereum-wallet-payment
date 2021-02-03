package io.horizontalsystems.bankwallet.modules.market.discovery

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.sort
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class MarketDiscoveryViewModel(
        private val service: MarketDiscoveryService,
        private val connectivityManager: ConnectivityManager
) : ViewModel() {

    val sortingFields: Array<SortingField> = SortingField.values()
    val marketCategories: List<MarketCategory> by service::marketCategories

    var sortingField: SortingField = sortingFields.first()
        private set

    var marketField: MarketField = MarketField.MarketCap
        private set

    val marketViewItemsLiveData = MutableLiveData<List<MarketViewItem>>()

    val loadingLiveData = MutableLiveData(false)
    val errorLiveData = MutableLiveData<String?>(null)

    val networkNotAvailable = SingleLiveEvent<Unit>()

    private val disposables = CompositeDisposable()

    init {
        service.stateObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    syncState(it)
                }
                .let {
                    disposables.add(it)
                }
    }

    private fun syncState(state: MarketDiscoveryService.State) {
        loadingLiveData.postValue(state is MarketDiscoveryService.State.Loading)

        if (state is MarketDiscoveryService.State.Error && !connectivityManager.isConnected) {
            networkNotAvailable.postValue(Unit)
        }

        errorLiveData.postValue((state as? MarketDiscoveryService.State.Error)?.error?.let { convertErrorMessage(it) })

        if (state is MarketDiscoveryService.State.Loaded) {
            syncViewItemsBySortingField()
        }
    }

    private fun convertErrorMessage(it: Throwable): String {
        return it.message ?: it.javaClass.simpleName
    }

    fun update(sortingField: SortingField? = null, marketField: MarketField? = null) {
        sortingField?.let {
            this.sortingField = it
        }
        marketField?.let {
            this.marketField = it
        }
        syncViewItemsBySortingField()
    }

    private fun syncViewItemsBySortingField() {
        val viewItems = service.marketItems
                .sort(sortingField)
                .map {
                    MarketViewItem.create(it, service.currency.symbol, marketField)
                }

        marketViewItemsLiveData.postValue(viewItems)
    }

    fun refresh() {
        service.refresh()
    }

    fun onErrorClick() {
        service.refresh()
    }

    fun onSelectMarketCategory(marketCategory: MarketCategory?) {
        service.marketCategory = marketCategory
    }

}
