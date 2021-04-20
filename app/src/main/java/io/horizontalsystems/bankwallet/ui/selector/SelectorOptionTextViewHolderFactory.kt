package io.horizontalsystems.bankwallet.ui.selector

import android.view.ViewGroup
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate

class SelectorOptionTextViewHolderFactory<ItemClass> : ItemViewHolderFactory<ItemViewHolder<ViewItemWrapper<ItemClass>>> {
    override fun create(parent: ViewGroup, viewType: Int): ItemViewHolder<ViewItemWrapper<ItemClass>> {
        return SelectorOptionTextViewHolder(inflate(parent, R.layout.view_holder_item_selector))
    }
}
