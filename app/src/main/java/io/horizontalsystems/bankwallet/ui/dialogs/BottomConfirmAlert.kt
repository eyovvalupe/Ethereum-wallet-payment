package io.horizontalsystems.bankwallet.ui.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_confirmation.*

class BottomConfirmAlert : DialogFragment(), ConfirmationsAdapter.Listener {

    interface Listener {
        fun onConfirmationSuccess()
    }

    private var mDialog: Dialog? = null
    private lateinit var rootView: View
    private lateinit var btnConfirm: Button
    private var checkboxItemList: MutableList<CheckBoxItem> = mutableListOf()
    private var adapter = ConfirmationsAdapter(this)
    private lateinit var recyclerView: RecyclerView

    private lateinit var listener: Listener
    private lateinit var color: BottomButtonColor

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = activity?.let { AlertDialog.Builder(it, R.style.BottomDialog) }

        rootView = View.inflate(context, R.layout.fragment_bottom_confirmations, null) as ViewGroup
        btnConfirm = rootView.findViewById(R.id.btnConfirm)
        recyclerView = rootView.findViewById(R.id.recyclerView)
        builder?.setView(rootView)

        mDialog = builder?.create()
        mDialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog?.window?.setGravity(Gravity.BOTTOM)
        mDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnConfirm.setOnClickListener {
            listener.onConfirmationSuccess()
            dismiss()
        }

        btnConfirm.setBackgroundResource(getBackgroundResId(color))
        setButtonTextColor(color, false)

        return mDialog as Dialog
    }

    private fun setButtonTextColor(buttonColor: BottomButtonColor, enabled: Boolean) {
        val colorRes: Int = when {
            enabled -> when (buttonColor) {
                BottomButtonColor.RED -> R.color.white
                BottomButtonColor.YELLOW -> R.color.black
            }
            else -> R.color.grey_50
        }

        context?.let {
            btnConfirm.setTextColor(ContextCompat.getColor(it, colorRes))
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter.confirmations = checkboxItemList
        adapter.notifyDataSetChanged()
    }

    override fun onItemCheckMarkClick(position: Int, checked: Boolean) {
        checkboxItemList[position].checked = checked
        checkConfirmations()
    }

    private fun getBackgroundResId(color: BottomButtonColor): Int = when (color) {
        BottomButtonColor.RED -> R.drawable.button_red_background_12
        BottomButtonColor.YELLOW -> R.drawable.button_yellow_background_12
    }

    private fun checkConfirmations() {
        val uncheckedCount = checkboxItemList.asSequence().filter { !it.checked }.count()
        val isEnabled = uncheckedCount == 0
        btnConfirm.isEnabled = isEnabled
        setButtonTextColor(color, isEnabled)
    }

    companion object {
        fun show(activity: FragmentActivity, textResourcesList: MutableList<Int>, listener: Listener, color: BottomButtonColor = BottomButtonColor.YELLOW) {
            val fragment = BottomConfirmAlert()
            fragment.listener = listener
            fragment.color = color
            textResourcesList.forEach {
                fragment.checkboxItemList.add(CheckBoxItem(it))
            }
            val ft = activity.supportFragmentManager.beginTransaction()
            ft.add(fragment, "bottom_confirm_alert")
            ft.commitAllowingStateLoss()
        }
    }
}

enum class BottomButtonColor {
    YELLOW, RED
}

class ConfirmationsAdapter(private var listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onItemCheckMarkClick(position: Int, checked: Boolean)
    }

    var confirmations: List<CheckBoxItem> = listOf()

    override fun getItemCount() = confirmations.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolderConfirmation(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_confirmation, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderConfirmation -> holder.bind(confirmations[position]) { checked ->
                listener.onItemCheckMarkClick(position, checked)
                notifyDataSetChanged()
            }
        }
    }
}

class ViewHolderConfirmation(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(checkBoxItem: CheckBoxItem, onClick: ((Boolean) -> (Unit))) {
        confirmationCheckBox.text = containerView.context.getString(checkBoxItem.textRes)
        confirmationCheckBox.isChecked = checkBoxItem.checked

        confirmationCheckBox.setOnCheckedChangeListener { _, _ ->
            onClick.invoke(confirmationCheckBox.isChecked)
        }
    }
}

class CheckBoxItem(val textRes: Int, var checked: Boolean = false)
