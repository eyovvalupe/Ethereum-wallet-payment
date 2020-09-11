package io.horizontalsystems.bankwallet.modules.swap.view.item

data class TradeViewItem(
        val price: String? = null,
        val priceImpact: String? = null,
        val minMaxTitle: String? = null,
        val minMaxAmount: String? = null
) {
    val isEmpty: Boolean
        get() = price == null && priceImpact == null && minMaxAmount == null
}
