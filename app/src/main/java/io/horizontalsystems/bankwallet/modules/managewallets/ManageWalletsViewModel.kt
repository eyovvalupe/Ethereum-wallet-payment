package io.horizontalsystems.bankwallet.modules.managewallets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsService.ItemState.Supported
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsService.ItemState.Unsupported
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewItem
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewItemState
import io.horizontalsystems.views.ListPosition
import io.reactivex.disposables.CompositeDisposable

class ManageWalletsViewModel(
    private val service: ManageWalletsService,
    private val clearables: List<Clearable>
) : ViewModel() {

    val viewItemsLiveData = MutableLiveData<List<CoinViewItem>>()
    val disableCoinLiveData = MutableLiveData<String>()

    private var disposables = CompositeDisposable()

    init {
        service.itemsObservable
            .subscribeIO { sync(it) }
            .let { disposables.add(it) }

        service.cancelEnableCoinObservable
            .subscribeIO { disableCoinLiveData.postValue(it.uid) }
            .let { disposables.add(it) }

        sync(service.items)
    }

    private fun sync(items: List<ManageWalletsService.Item>) {
        val itemsSize = items.size
        val viewItems = items.mapIndexed { index, item ->
            viewItem(item, ListPosition.getListPosition(itemsSize, index))
        }
        viewItemsLiveData.postValue(viewItems)
    }

    private fun viewItem(
        item: ManageWalletsService.Item,
        listPosition: ListPosition
    ): CoinViewItem {
        val state = when (item.state) {
            is Supported -> CoinViewItemState.ToggleVisible(
                item.state.enabled,
                item.state.hasSettings
            )
            is Unsupported -> CoinViewItemState.ToggleHidden
        }
        return CoinViewItem(
            item.fullCoin.coin.uid,
            ImageSource.Remote(item.fullCoin.coin.iconUrl, item.fullCoin.iconPlaceholder),
            item.fullCoin.coin.name,
            item.fullCoin.coin.code,
            state,
            listPosition
        )
    }

    fun enable(uid: String) {
        service.enable(uid)
    }

    fun disable(uid: String) {
        service.disable(uid)
    }

    fun onClickSettings(uid: String) {
        service.configure(uid)
    }

    fun updateFilter(filter: String) {
        service.setFilter(filter)
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

}
