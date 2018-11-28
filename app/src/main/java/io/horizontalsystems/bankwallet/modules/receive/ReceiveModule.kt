package io.horizontalsystems.bankwallet.modules.receive

import android.support.v4.app.FragmentActivity
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem
import io.horizontalsystems.bankwallet.modules.transactions.Coin
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper

object ReceiveModule {

    interface IView {
        fun showAddresses(addresses: List<AddressItem>)
        fun showError(error: Int)
        fun showCopied()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onCopyClick(index: Int)
    }

    interface IInteractor {
        fun getReceiveAddress()
        fun copyToClipboard(coinAddress: String)
    }

    interface IInteractorDelegate {
        fun didReceiveAddresses(addresses: List<AddressItem>)
        fun didFailToReceiveAddress(exception: Exception)
        fun didCopyToClipboard()
    }

    fun init(view: ReceiveViewModel, coin: Coin?) {
        val interactor = ReceiveInteractor(coin, App.walletManager, TextHelper)
        val presenter = ReceivePresenter(interactor)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(activity: FragmentActivity, coin: Coin) {
        ReceiveFragment.show(activity, coin)
    }

}
