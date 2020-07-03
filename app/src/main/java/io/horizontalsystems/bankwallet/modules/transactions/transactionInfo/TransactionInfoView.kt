package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.TransactionType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.info.InfoModule
import io.horizontalsystems.bankwallet.ui.extensions.ConstraintLayoutWithHeader
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.transaction_info_bottom_sheet.view.*

class TransactionInfoView : ConstraintLayoutWithHeader {

    private lateinit var viewModel: TransactionInfoViewModel
    private lateinit var lifecycleOwner: LifecycleOwner
    private var listener: Listener? = null

    interface Listener {
        fun openTransactionInfo()
        fun openFullTransactionInfo(transactionHash: String, wallet: Wallet)
        fun closeTransactionInfo()
        fun onShowInfoMessage(snackbar: Snackbar? = null)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(viewModel: TransactionInfoViewModel, lifecycleOwner: LifecycleOwner, listener: Listener) {
        setContentView(R.layout.transaction_info_bottom_sheet)

        this.viewModel = viewModel
        this.listener = listener
        this.lifecycleOwner = lifecycleOwner
        setTransactionInfoDialog()
    }

    private fun setTransactionInfoDialog() {
        setOnCloseCallback { listener?.closeTransactionInfo() }

        txtFullInfo.setOnSingleClickListener {
            viewModel.delegate.openFullInfo()
        }

        val transactionDetailsAdapter = TransactionDetailsAdapter(viewModel)
        rvDetails.adapter = transactionDetailsAdapter

        viewModel.showCopiedLiveEvent.observe(lifecycleOwner, Observer {
            val snackbar = HudHelper.showSuccessMessage(this, R.string.Hud_Text_Copied, gravity = HudHelper.SnackbarGravity.TOP_OF_VIEW)
            listener?.onShowInfoMessage(snackbar)
        })

        viewModel.showShareLiveEvent.observe(lifecycleOwner, Observer { url ->
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, url)
                type = "text/plain"
            }
            context.startActivity(sendIntent)
        })


        viewModel.showFullInfoLiveEvent.observe(lifecycleOwner, Observer { pair ->
            pair?.let {
                listener?.openFullTransactionInfo(transactionHash = it.first, wallet = it.second)
            }
        })

        viewModel.showLockInfo.observe(lifecycleOwner, Observer { lockDate ->
            val title = context.getString(R.string.Info_LockTime_Title)
            val description = context.getString(R.string.Info_LockTime_Description, DateHelper.getFullDate(lockDate))

            InfoModule.start(context, InfoModule.InfoParameters(title, description))
        })

        viewModel.showDoubleSpendInfo.observe(lifecycleOwner, Observer { (txHash, conflictingTxHash) ->
            val title = context.getString(R.string.Info_DoubleSpend_Title)
            val description = context.getString(R.string.Info_DoubleSpend_Description)

            InfoModule.start(context, InfoModule.InfoParameters(title, description, txHash, conflictingTxHash))
        })

        viewModel.titleLiveData.observe(lifecycleOwner, Observer { titleViewItem ->
            val incoming = titleViewItem.type == TransactionType.Incoming
            val sentToSelf = titleViewItem.type == TransactionType.SentToSelf

            setTitle(context.getString(R.string.TransactionInfo_Title))
            setSubtitle(titleViewItem.date?.let { DateHelper.getFullDate(it) })
            setHeaderIcon(if (incoming) R.drawable.ic_incoming else R.drawable.ic_outgoing)

            sentToSelfIcon.visibility = if (sentToSelf) View.VISIBLE else View.GONE

            val lockIcon = when {
                titleViewItem.lockState == null -> 0
                titleViewItem.lockState.locked -> R.drawable.ic_lock
                else -> R.drawable.ic_unlock
            }
            primaryValue.setCompoundDrawablesWithIntrinsicBounds(0, 0, lockIcon, 0)

            val amountTextColor = if (incoming) R.color.green_d else R.color.yellow_d
            primaryValue.setTextColor(context.getColor(amountTextColor))

            titleViewItem.primaryAmountInfo.let {
                primaryName.text = it.getAmountName()
                primaryValue.text = it.getFormattedForTxInfo()
            }

            titleViewItem.secondaryAmountInfo.let {
                secondaryName.text = it?.getAmountName()
                secondaryValue.text = it?.getFormattedForTxInfo()
            }
        })

        viewModel.detailsLiveData.observe(lifecycleOwner, Observer {
            transactionDetailsAdapter.setItems(it)
            listener?.openTransactionInfo()
        })
    }

}
