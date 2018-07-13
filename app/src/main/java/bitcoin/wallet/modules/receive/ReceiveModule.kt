package bitcoin.wallet.modules.receive

import android.support.v4.app.FragmentActivity
import bitcoin.wallet.entities.coins.Coin

object ReceiveModule {

    interface IView

    interface IViewDelegate

    interface IInteractor

    interface IInteractorDelegate

    interface IRouter

    fun init(view: IView, router: IRouter) {

    }

    fun start(activity: FragmentActivity, coin: Coin, address: String) {
        ReceiveFragment.show(activity, coin, address)
    }

}
