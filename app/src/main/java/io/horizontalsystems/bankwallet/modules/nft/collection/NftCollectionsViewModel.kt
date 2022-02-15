package io.horizontalsystems.bankwallet.modules.nft.collection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class NftCollectionsViewModel(private val service: NftCollectionsService) : ViewModel() {
    val priceType by service::priceType

    var viewState by mutableStateOf<ViewState?>(null)
        private set
    var loading by mutableStateOf(false)
        private set

    var collections by mutableStateOf<List<NftCollectionViewItem>>(listOf())
        private set

    init {
        viewModelScope.launch {
            service.nftCollections
                .collect {
                    handleNftCollections(it)
                }
        }

        service.start()
    }

    private fun handleNftCollections(nftCollectionsState: DataState<List<NftCollectionItem>>) {
        loading = nftCollectionsState.loading

        nftCollectionsState.dataOrNull?.let {
            viewState = ViewState.Success

            syncItems(it)
        }
    }

    private fun syncItems(collectionItems: List<NftCollectionItem>) {
        val expandedStates = this.collections.map {
            it.slug to it.expanded
        }.toMap()

        this.collections = collectionItems.map { nftCollectionItem ->
            NftCollectionViewItem(
                slug = nftCollectionItem.slug,
                name = nftCollectionItem.name,
                imageUrl = nftCollectionItem.imageUrl,
                ownedAssetCount = nftCollectionItem.ownedAssetCount,
                assets = nftCollectionItem.assets,
                expanded = expandedStates[nftCollectionItem.slug] ?: false
            )
        }
    }

    override fun onCleared() {
        service.stop()
    }

    fun refresh() {
        viewModelScope.launch {
            loading = true
            service.refresh()
            loading = false
        }
    }

    fun toggleCollection(collection: NftCollectionViewItem) {
        val index = collections.indexOf(collection)

        if (index != -1) {
            collections = collections.toMutableList().apply {
                this[index] = collection.copy(expanded = !collection.expanded)
            }
        }
    }

    fun updatePriceType(priceType: PriceType) {
        service.updatePriceType(priceType)
    }

}
