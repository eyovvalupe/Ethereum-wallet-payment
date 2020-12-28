package io.horizontalsystems.bankwallet.modules.managewallets

import android.os.Handler
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.blockchainsettings.BlockchainSettingsService
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewItem
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewState
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class ManageWalletsViewModel(
        private val service: ManageWalletsModule.IManageWalletsService,
        private val blockchainSettingsService: BlockchainSettingsService,
        private val clearables: List<Clearable>
) : ViewModel() {

    val viewStateLiveData = MutableLiveData<CoinViewState>()

    private var disposables = CompositeDisposable()
    private var filter: String? = null

    init {
        Handler().postDelayed({
            syncViewState()

            service.stateAsync
                    .subscribeOn(Schedulers.io())
                    .subscribe {
                        syncViewState(it)
                    }
                    .let { disposables.add(it) }

            blockchainSettingsService.approveEnableCoinAsync
                    .subscribeOn(Schedulers.io())
                    .subscribe {
                        service.enable(it)
                    }
                    .let { disposables.add(it) }

            blockchainSettingsService.rejectEnableCoinAsync
                    .subscribeOn(Schedulers.io())
                    .subscribe {
                        syncViewState()
                    }
                    .let { disposables.add(it) }

        }, 500)
    }

    override fun onCleared() {
        disposables.clear()
        clearables.forEach {
            it.clear()
        }
        super.onCleared()
    }

    fun enable(coin: Coin) {
        val account = service.account(coin) ?: return
        blockchainSettingsService.approveEnable(coin, account.origin)
    }

    fun disable(coin: Coin) {
        service.disable(coin)
    }

    fun updateFilter(newText: String?) {
        filter = newText
        syncViewState()
    }

    private fun syncViewState(updatedState: ManageWalletsModule.State? = null) {
        val state = updatedState ?: service.state

        val filteredFeatureCoins = filtered(state.featuredItems)

        val filteredItems = filtered(state.items)

        viewStateLiveData.postValue(CoinViewState(
                filteredFeatureCoins.mapIndexed { index, item ->
                    viewItem(item, filteredFeatureCoins.size - 1 == index)
                },
                filteredItems.mapIndexed { index, item ->
                    viewItem(item, filteredItems.size - 1 == index)
                }
        ))
    }

    private fun viewItem(item: ManageWalletsModule.Item, last: Boolean): CoinViewItem {
        return when (val itemState = item.state) {
            ManageWalletsModule.ItemState.NoAccount -> CoinViewItem.ToggleHidden(item.coin, last)
            is ManageWalletsModule.ItemState.HasAccount -> CoinViewItem.ToggleVisible(item.coin, itemState.hasWallet, last)
        }
    }

    private fun filtered(items: List<ManageWalletsModule.Item>): List<ManageWalletsModule.Item> {
        val filter = filter ?: return items

        return items.filter {
            it.coin.title.toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH))
                    || it.coin.code.toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH))
        }
    }

}
