package io.horizontalsystems.bankwallet.ui.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IPredefinedAccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper

class ManageWalletsDialog(
        private val listener: Listener,
        private val coin: Coin,
        private val predefinedAccountType: IPredefinedAccountType)
    : BaseBottomSheetDialogFragment() {

    interface Listener {
        fun onClickCreateKey()
        fun onClickRestoreKey() {}
        fun onCancel() {}
    }

    private lateinit var addKeyInfo: TextView
    private lateinit var btnCreateKey: Button
    private lateinit var btnRestoreKey: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.fragment_bottom_manage_keys)

        setTitle(activity?.getString(R.string.Deposit_Title, coin.code))
        setSubtitle(coin.title)
        setHeaderIcon(LayoutHelper.getCoinDrawableResource(coin.code))

        addKeyInfo = view.findViewById(R.id.addKeyInfo)
        btnCreateKey = view.findViewById(R.id.btnYellow)
        btnRestoreKey = view.findViewById(R.id.btnGrey)

        addKeyInfo.text = getString(
                R.string.AddCoin_Description, "${coin.title} (${coin.code})",
                getString(predefinedAccountType.coinCodes),
                getString(predefinedAccountType.title)
        )

        bindActions()
    }

    override fun close() {
        super.close()
        listener.onCancel()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener.onCancel()
    }

    private fun bindActions() {
        btnCreateKey.setOnClickListener {
            listener.onClickCreateKey()
            dismiss()
        }

        btnRestoreKey.setOnClickListener {
            listener.onClickRestoreKey()
            dismiss()
        }
    }

    companion object {
        fun show(activity: FragmentActivity, listener: Listener, coin: Coin, predefinedAccountType: IPredefinedAccountType) {
            val fragment = ManageWalletsDialog(listener, coin, predefinedAccountType)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_manage_keys_dialog")
            transaction.commitAllowingStateLoss()
        }
    }
}
