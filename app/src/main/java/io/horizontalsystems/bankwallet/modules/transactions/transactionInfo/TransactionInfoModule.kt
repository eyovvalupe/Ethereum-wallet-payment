package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import java.util.*

object TransactionInfoModule {
    interface View {
        fun showCopied()
    }

    interface ViewDelegate {
        fun onCopy(value: String)
        fun openFullInfo(transactionHash: String, coin: Wallet)
        fun onClickLockInfo(lockDate: Date)
    }

    interface Interactor {
        fun onCopy(value: String)
    }

    interface InteractorDelegate

    interface Router {
        fun openFullInfo(transactionHash: String, wallet: Wallet)
        fun openLockInfo(lockDate: Date)
    }

    fun init(view: TransactionInfoViewModel, router: Router) {
        val interactor = TransactionInfoInteractor(TextHelper)
        val presenter = TransactionInfoPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
