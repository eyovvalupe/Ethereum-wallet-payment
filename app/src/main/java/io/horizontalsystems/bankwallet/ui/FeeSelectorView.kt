package io.horizontalsystems.bankwallet.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ethereum.ISendFeePriorityViewModel
import io.horizontalsystems.bankwallet.core.ethereum.ISendFeeViewModel
import io.horizontalsystems.bankwallet.core.ethereum.SendFeeSliderViewItem
import io.horizontalsystems.bankwallet.core.ethereum.SendPriorityViewItem
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.seekbar.FeeSeekBar
import kotlinx.android.synthetic.main.view_send_fee.view.*

class FeeSelectorView@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {

    var onTxSpeedClickListener: (view: View) -> Unit = { }
    var prioritySelectListener: (position: Int) -> Unit = { }
    var customFeeSeekBarListener: (value: Int) -> Unit = { }

    init {
        inflate(context, R.layout.view_send_fee, this)

        txSpeedLayout.setOnClickListener {
            onTxSpeedClickListener(it)
        }

        customFeeSeekBar.setListener(object : FeeSeekBar.Listener {
            override fun onSelect(value: Int) {
                customFeeSeekBarListener(value)
            }
        })
    }

    fun setFeeText(value: String) {
        txFeePrimary.text = value
        invalidate()
    }

    fun setDurationVisible(visible: Boolean) {
        txDurationLayout.isVisible = visible
        invalidate()
    }

    fun setPriorityText(value: String) {
        txSpeedMenu.text = value
        invalidate()
    }

    fun setFeeSliderViewItem(viewItem: SendFeeSliderViewItem?) {
        customFeeSeekBar.isVisible = viewItem != null

        viewItem?.let {
            customFeeSeekBar.min = it.range.lower.toInt()
            customFeeSeekBar.max = it.range.upper.toInt()
            customFeeSeekBar.progress = it.initialValue.toInt()
            customFeeSeekBar.setBubbleHint(it.unit)
        }

        invalidate()
    }

    fun openPrioritySelector(items: List<SendPriorityViewItem>, fragmentManager: FragmentManager) {
        val selectorItems = items.map { feeRateViewItem ->
            SelectorItem(feeRateViewItem.title, feeRateViewItem.selected)
        }

        SelectorDialog
                .newInstance(selectorItems, context.getString(R.string.Send_DialogSpeed)) { position ->
                    prioritySelectListener(position)
                }
                .show(fragmentManager, "fee_rate_priority_selector")
    }

    fun setFeeSelectorViewInteractions(sendFeeViewModel: ISendFeeViewModel, sendFeePriorityViewModel: ISendFeePriorityViewModel, viewLifecycleOwner: LifecycleOwner, fragmentManager: FragmentManager) {
        sendFeeViewModel.feeLiveData.observe(viewLifecycleOwner, Observer {
            setFeeText(it)
        })

        sendFeePriorityViewModel.priorityLiveData.observe(viewLifecycleOwner, Observer {
            setPriorityText(it)
        })

        sendFeePriorityViewModel.openSelectPriorityLiveEvent.observe(viewLifecycleOwner, Observer {
            openPrioritySelector(it, fragmentManager)
        })

        sendFeePriorityViewModel.feeSliderLiveData.observe(viewLifecycleOwner, {
            setFeeSliderViewItem(it)
        })

        onTxSpeedClickListener = {
            sendFeePriorityViewModel.openSelectPriority()
        }

        prioritySelectListener = { position ->
            sendFeePriorityViewModel.selectPriority(position)
        }

        customFeeSeekBarListener = {
            sendFeePriorityViewModel.changeCustomPriority(it.toLong())
        }
    }

}