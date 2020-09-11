package io.horizontalsystems.bankwallet.modules.contact

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper

object ContactModule {
    interface IView {
        fun setEmail(email: String)
        fun setWalletHelpTelegramGroup(group: String)
        fun showCopied()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun didTapEmail()
        fun didTapWalletHelpTelegram()
    }

    interface IInteractor {
        val email: String
        val walletHelpTelegramGroup: String
        fun copyToClipboard(value: String)
    }

    interface IRouter {
        fun openSendMail(recipient: String)
        fun openTelegram(group: String)
    }

    interface IRouterDelegate {
        fun didFailSendMail()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = ContactView()
            val router = ContactRouter()
            val interactor = ContactInteractor(App.appConfigProvider, TextHelper)
            val presenter = ContactPresenter(view, router, interactor)

            return presenter as T
        }
    }

    fun start(context: Activity) {
        val intent = Intent(context, ContactActivity::class.java)
        context.startActivity(intent)
    }
}
