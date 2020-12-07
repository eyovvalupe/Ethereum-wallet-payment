package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import java.util.*

object TransactionInfoModule {
    interface View {
        fun showCopied()
        fun share(value: String)
        fun showTitle(titleViewItem: TitleViewItem)
        fun showDetails(items: List<TransactionDetailViewItem>)
    }

    interface ViewDelegate {
        fun viewDidLoad()
        fun onShare()
        fun openFullInfo()
        fun onClickLockInfo()
        fun onClickDoubleSpendInfo()
        fun onClickRecipientHash()
        fun onClickTo()
        fun onClickFrom()
        fun onClickTransactionId()
        fun onRawTransaction()
        fun onClickStatusInfo()
    }

    interface Interactor {
        val lastBlockInfo: LastBlockInfo?

        fun copyToClipboard(value: String)
        fun getRate(code: String, timestamp: Long): CurrencyValue?
        fun feeCoin(coin: Coin): Coin?
        fun getRaw(transactionHash: String): String?
    }

    interface InteractorDelegate

    interface Router {
        fun openFullInfo(transactionHash: String, wallet: Wallet)
        fun openLockInfo(lockDate: Date)
        fun openDoubleSpendInfo(transactionHash: String, conflictingTxHash: String)
        fun openStatusInfo()
    }

    fun init(view: TransactionInfoViewModel, router: Router, transactionRecord: TransactionRecord, wallet: Wallet) {
        val adapter = App.adapterManager.getTransactionsAdapterForWallet(wallet)!!
        val interactor = TransactionInfoInteractor(TextHelper, adapter, App.xRateManager, App.currencyManager, App.feeCoinProvider)
        val presenter = TransactionInfoPresenter(interactor, router, transactionRecord, wallet, TransactionInfoAddressMapper)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    data class TitleViewItem(val date: Date?, val primaryAmountInfo: SendModule.AmountInfo, val secondaryAmountInfo: SendModule.AmountInfo?, val type: TransactionType, val lockState: TransactionLockState?)
}
