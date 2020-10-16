package io.horizontalsystems.bankwallet.modules.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.SendActivity
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionInfoView
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionInfoViewModel
import io.horizontalsystems.snackbar.CustomSnackbar
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity(), TransactionInfoView.Listener {

    private var txInfoViewModel: TransactionInfoViewModel? = null
    private var txInfoBottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var messageInfoSnackbar: CustomSnackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null) // null prevents fragments restoration on theme switch

        setContentView(R.layout.activity_main)
        setTransparentStatusBar()

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainerView, MainFragment.new(intent.extras?.getInt(ACTIVE_TAB_KEY)))
            commit()
        }

        preloadBottomSheets()
    }

    override fun onResume() {
        super.onResume()
        collapseBottomSheetsOnActivityRestore()
    }

    override fun onBackPressed() {
        supportFragmentManager.fragments.lastOrNull()?.let { fragment ->
            if ((fragment as? BaseFragment)?.canHandleOnBackPress() == true) {
                return
            }
        }
        when (txInfoBottomSheetBehavior?.state) {
            BottomSheetBehavior.STATE_EXPANDED -> {
                txInfoBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            }
            else -> super.onBackPressed()
        }
    }

    override fun onTrimMemory(level: Int) {
        when (level){
            TRIM_MEMORY_RUNNING_MODERATE,
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                /*
                   Release any memory that your app doesn't need to run.

                   The device is running low on memory while the app is running.
                   The event raised indicates the severity of the memory-related event.
                   If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
                   begin killing background processes.
                */
                if (App.backgroundManager.inBackground) {
                    val logger = AppLogger("low memory")
                    logger.info("Kill activity due to low memory, level: $level")
                    finishAffinity()
                }
            }
            else -> {  /*do nothing*/ }
        }

        super.onTrimMemory(level)
    }

    fun openSend(wallet: Wallet) {
        val intent = Intent(this, SendActivity::class.java).apply {
            putExtra(SendActivity.WALLET, wallet)
        }
        startActivity(intent)
    }

    //  TransactionInfo bottomsheet

    fun openTransactionInfo(transactionRecord: TransactionRecord, wallet: Wallet) {
        txInfoViewModel?.init(transactionRecord, wallet)
    }

    override fun openTransactionInfo() {
        txInfoBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun closeTransactionInfo() {
        txInfoBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun onShowInfoMessage(snackbar: CustomSnackbar?) {
        this.messageInfoSnackbar = snackbar
    }

    override fun showFragmentInTopContainerView(fragment: Fragment) {
        supportFragmentManager.commit {
            add(R.id.topFragmentContainerView, fragment)
            addToBackStack(null)
        }
    }

    private fun preloadBottomSheets() {
        Handler().postDelayed({
            setBottomSheets()

            txInfoBottomSheetBehavior = BottomSheetBehavior.from(transactionInfoNestedScrollView)
            setBottomSheet(txInfoBottomSheetBehavior)

            txInfoViewModel = ViewModelProvider(this).get(TransactionInfoViewModel::class.java)
            txInfoViewModel?.let {
                transactionInfoView.bind(it, this, this)
            }
        }, 200)
    }

    private fun setBottomSheets() {
        hideDim()

        bottomSheetDim.setOnClickListener {
            txInfoBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun collapseBottomSheetsOnActivityRestore() {
        if (txInfoBottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED && findViewById<TextView>(R.id.secondaryName)?.text?.isEmpty() == true) {
            txInfoBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun setBottomSheet(bottomSheetBehavior: BottomSheetBehavior<View>?) {
        bottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.isFitToContents = true
                    bottomSheetDim.alpha = 1f
                    bottomSheetDim.isVisible = true
                } else {
                    messageInfoSnackbar?.dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                bottomSheetDim.alpha = slideOffset
                bottomSheetDim.isGone = slideOffset == 0f
            }
        })
    }

    private fun hideDim() {
        bottomSheetDim.isGone = true
        bottomSheetDim.alpha = 0f
    }

    companion object {
        const val ACTIVE_TAB_KEY = "active_tab"
        const val SETTINGS_TAB_POSITION = 3
    }
}
