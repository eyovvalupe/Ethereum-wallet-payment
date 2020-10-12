package io.horizontalsystems.bankwallet.modules.transactions

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.TransactionType
import kotlinx.android.synthetic.main.view_transaction_progress.view.*


class TransactionProgressView : ConstraintLayout {

    constructor(context: Context) : super(context) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initializeViews()
    }

    private fun initializeViews() {
        inflate(context, R.layout.view_transaction_progress, this)
    }

    fun bind(progress: Double = 0.0, type: TransactionType) {
        when {
            progress <= 0.33 -> progressBar.setBackgroundResource(R.drawable.animation_pending)
            progress <= 0.66 -> progressBar.setBackgroundResource(getOneConfirmationBackground(type))
            progress <= 1.0 -> progressBar.setBackgroundResource(getTwoConfirmationBackground(type))
        }
        processText.setText(getDescription(type))
        (progressBar.background as? AnimationDrawable)?.start()
    }

    private fun getOneConfirmationBackground(type: TransactionType) = when (type) {
        TransactionType.Outgoing,  TransactionType.SentToSelf -> R.drawable.animation_sending_progress_30
        TransactionType.Incoming ->  R.drawable.animation_receiving_progress_30
        TransactionType.Approve -> R.drawable.animation_approving_progress_30
    }

    private fun getTwoConfirmationBackground(type: TransactionType) = when (type) {
        TransactionType.Outgoing,  TransactionType.SentToSelf -> R.drawable.animation_sending_progress_60
        TransactionType.Incoming ->  R.drawable.animation_receiving_progress_60
        TransactionType.Approve -> R.drawable.animation_approving_progress_60
    }

    private fun getDescription(type: TransactionType) = when (type) {
        TransactionType.Outgoing, TransactionType.SentToSelf -> R.string.Transactions_Sending
        TransactionType.Incoming -> R.string.Transactions_Receiving
        TransactionType.Approve -> R.string.Transactions_Approving
    }

}
