package io.horizontalsystems.bankwallet.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.CoinIconView
import io.horizontalsystems.bankwallet.viewHelpers.bottomDialog

class ManageKeysDialog(private val listener: Listener, private val title: String, private val coinCodes: String)
    : DialogFragment() {

    interface Listener {
        fun onClickCreateKey()
    }

    private lateinit var rootView: View
    private lateinit var addKeyTitle: TextView
    private lateinit var addKeyInfo: TextView
    private lateinit var addCoinIcon: CoinIconView
    private lateinit var buttonCreateKey: Button
    private lateinit var buttonRestoreKey: Button

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        rootView = View.inflate(context, R.layout.fragment_bottom_manage_keys, null) as ViewGroup

        addKeyTitle = rootView.findViewById(R.id.addKeyTitle)
        addKeyInfo = rootView.findViewById(R.id.addKeyInfo)
        addCoinIcon = rootView.findViewById(R.id.addKeyIcon)
        buttonCreateKey = rootView.findViewById(R.id.btnCreateKey)
        buttonRestoreKey = rootView.findViewById(R.id.btnRestoreKey)

        bindContent()
        bindActions()

        return bottomDialog(activity, rootView)
    }

    private fun bindContent() {
        addCoinIcon.bind(R.drawable.ic_manage_keys)

        addKeyTitle.text = title
        addKeyInfo.text = getString(R.string.ManageCoins_AddCoin_Text, coinCodes)
    }

    private fun bindActions() {
        buttonRestoreKey.visibility = View.GONE
        buttonCreateKey.setOnClickListener {
            listener.onClickCreateKey()
            dismiss()
        }
    }

    companion object {
        fun show(activity: FragmentActivity, listener: Listener, title: String, coinCodes: String) {
            val fragment = ManageKeysDialog(listener, title, coinCodes)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_create_key_dialog")
            transaction.commitAllowingStateLoss()
        }
    }
}
