package io.horizontalsystems.bankwallet.modules.pin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.ui.extensions.*
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.synthetic.main.activity_pin.*
import java.util.concurrent.Executor


class PinActivity : BaseActivity(), NumPadItemsAdapter.Listener {

    private lateinit var viewModel: PinViewModel
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var pinPagesAdapter: PinPagesAdapter
    private val executor = Executor { command -> command.run() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        val interactionType = intent.getSerializableExtra(keyInteractionType) as PinInteractionType
        val showCancelButton = intent.getBooleanExtra(keyShowCancel, false)

        pinPagesAdapter = PinPagesAdapter()
        layoutManager = SmoothLinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        pinPagesRecyclerView.layoutManager = layoutManager
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(pinPagesRecyclerView)
        pinPagesRecyclerView.adapter = pinPagesAdapter

        pinPagesRecyclerView.setOnTouchListener { _, _ ->
            //disable recyclerview scroll
            true
        }


        viewModel = ViewModelProviders.of(this).get(PinViewModel::class.java)
        viewModel.init(interactionType, showCancelButton)

        val numpadAdapter = NumPadItemsAdapter(this, NumPadItemType.FINGER)

        numPadItemsRecyclerView.adapter = numpadAdapter
        numPadItemsRecyclerView.layoutManager = GridLayoutManager(this, 3)


        viewModel.hideToolbar.observe(this, Observer {
            shadowlessToolbar.visibility = View.GONE
        })

        viewModel.showBackButton.observe(this, Observer {
            shadowlessToolbar.bind(null, TopMenuItem(R.drawable.back, onClick = { viewModel.delegate.onBackPressed() }))
        })

        viewModel.titleLiveDate.observe(this, Observer { title ->
            title?.let {
                shadowlessToolbar.bindTitle(getString(it))
            }
        })

        viewModel.addPagesEvent.observe(this, Observer { pinPages ->
            pinPages?.let {
                pinPagesAdapter.pinPages.addAll(it)
                pinPagesAdapter.notifyDataSetChanged()
            }
        })

        viewModel.showPageAtIndex.observe(this, Observer { index ->
            index?.let {
                Handler().postDelayed({
                    pinPagesAdapter.setEnteredPinLength(layoutManager.findFirstVisibleItemPosition(), 0)
                    pinPagesRecyclerView.smoothScrollToPosition(it)
                }, 300)
            }
        })

        viewModel.showErrorForPage.observe(this, Observer { errorForPage ->
            errorForPage?.let { (error, pageIndex) ->
                pinPagesAdapter.setErrorForPage(pageIndex, error?.let { getString(error) } ?: null)
            }
        })

        viewModel.showError.observe(this, Observer { error ->
            error?.let {
                HudHelper.showErrorMessage(it)
            }
        })

        viewModel.navigateToMainLiveEvent.observe(this, Observer {
            MainModule.start(this)
            finish()
        })

        viewModel.fillPinCircles.observe(this, Observer { pair ->
            pair?.let { (length, pageIndex) ->
                pinPagesAdapter.setEnteredPinLength(pageIndex, length)
            }
        })

        viewModel.dismissWithCancelLiveEvent.observe(this, Observer {
            setResult(PinModule.RESULT_CANCELLED)
            finish()
        })

        viewModel.dismissWithSuccessLiveEvent.observe(this, Observer {
            setResult(PinModule.RESULT_OK)
            finish()
        })

        viewModel.showFingerprintInputLiveEvent.observe(this, Observer { cryptoObject ->
            cryptoObject?.let {
                showFingerprintDialog(it)
                numpadAdapter.showFingerPrintButton = true
            }
        })

        viewModel.resetCirclesWithShakeAndDelayForPage.observe(this, Observer { pageIndex ->
            pageIndex?.let {
                pinPagesAdapter.shakePageIndex = it
                pinPagesAdapter.notifyDataSetChanged()
                Handler().postDelayed({
                    pinPagesAdapter.shakePageIndex = null
                    pinPagesAdapter.setEnteredPinLength(pageIndex, 0)
                    viewModel.delegate.resetPin()
                }, 300)
            }
        })

        viewModel.showAttemptsLeftError.observe(this, Observer { pair ->
            pair?.let { (attempts, pageIndex) ->
                pinUnlock.visibility = View.VISIBLE
                pinUnlockBlocked.visibility = View.GONE
            }
        })

        viewModel.showLockedView.observe(this, Observer { untilDate ->
            untilDate?.let {
                pinUnlock.visibility = View.GONE
                pinUnlockBlocked.visibility = View.VISIBLE
                val time = DateHelper.formatDate(it, "HH:mm:ss")
                blockedScreenMessage.text = getString(R.string.UnlockPin_WalletDisabledUntil, time)
            }
        })

        viewModel.closeApplicationLiveEvent.observe(this, Observer {
            finishAffinity()
        })

    }

    override fun onBackPressed() {
        viewModel.delegate.onBackPressed()
    }

    override fun onItemClick(item: NumPadItem) {
        when (item.type) {
            NumPadItemType.NUMBER -> viewModel.delegate.onEnter(item.number.toString(), layoutManager.findFirstVisibleItemPosition())
            NumPadItemType.DELETE -> viewModel.delegate.onDelete(layoutManager.findFirstVisibleItemPosition())
            NumPadItemType.FINGER -> viewModel.delegate.showFingerprintUnlock()
        }
    }

    fun onFingerprintAuthSucceed() {
        viewModel.delegate.onFingerprintUnlock()
    }

    private fun showFingerprintDialog(cryptoObject: BiometricPrompt.CryptoObject) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.Fingerprint_DialogTitle))
                .setNegativeButtonText(getString(R.string.Button_Cancel))
                .build()

        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onFingerprintAuthSucceed()
            }
        })

        biometricPrompt.authenticate(promptInfo, cryptoObject)
    }

    companion object {

        private const val keyInteractionType = "interaction_type"
        private const val keyShowCancel = "show_cancel"

        fun start(context: Context, interactionType: PinInteractionType) {
            val intent = Intent(context, PinActivity::class.java)
            intent.putExtra(keyInteractionType, interactionType)
            context.startActivity(intent)
        }

        fun startForResult(context: AppCompatActivity, requestCode: Int, interactionType: PinInteractionType, showCancel: Boolean = true) {
            val intent = Intent(context, PinActivity::class.java)
            intent.putExtra(keyInteractionType, interactionType)
            intent.putExtra(keyShowCancel, showCancel)

            context.startActivityForResult(intent, requestCode)
        }
    }
}

//PinPage part
class PinPage(val description: Int, var enteredDidgitsLength: Int = 0, var error: String? = null)

class PinPagesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var pinPages = mutableListOf<PinPage>()
    var shakePageIndex: Int? = null

    fun setErrorForPage(pageIndex: Int, error: String?) {
        pinPages[pageIndex].error = error
        notifyDataSetChanged()
    }

    fun setEnteredPinLength(pageIndex: Int, enteredLength: Int) {
        pinPages[pageIndex].enteredDidgitsLength = enteredLength
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return PinPageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_pin_page, parent, false))
    }

    override fun getItemCount() = pinPages.count()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PinPageViewHolder) {
            holder.bind(pinPages[position], shakePageIndex == position)//, { listener.onChangeProvider(numPadItems[position]) }, listener.isBiometricEnabled())
        }
    }

}

class PinPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var txtDesc: TextView = itemView.findViewById(R.id.txtDescription)
    private var txtError: TextView = itemView.findViewById(R.id.errorMessage)
    private var pinCirclesWrapper = itemView.findViewById<ConstraintLayout>(R.id.pinCirclesWrapper)

    private var imgPinMask1: ImageView = itemView.findViewById(R.id.imgPinMaskOne)
    private var imgPinMask2: ImageView = itemView.findViewById(R.id.imgPinMaskTwo)
    private var imgPinMask3: ImageView = itemView.findViewById(R.id.imgPinMaskThree)
    private var imgPinMask4: ImageView = itemView.findViewById(R.id.imgPinMaskFour)
    private var imgPinMask5: ImageView = itemView.findViewById(R.id.imgPinMaskFive)
    private var imgPinMask6: ImageView = itemView.findViewById(R.id.imgPinMaskSix)

    fun bind(pinPage: PinPage, shake: Boolean) {
        txtDesc.text = itemView.resources.getString(pinPage.description)
        updatePinCircles(pinPage.enteredDidgitsLength)
        txtError.text = pinPage.error ?: ""
        if (shake) {
            val shakeAnim = AnimationUtils.loadAnimation(itemView.context, R.anim.shake_pin_circles)
            pinCirclesWrapper.startAnimation(shakeAnim)
        }
    }

    private fun updatePinCircles(length: Int) {
        val filledCircle = R.drawable.pin_circle_filled
        val emptyCircle = R.drawable.ic_circle_steel_20_with_border

        imgPinMask1.setImageResource(if (length > 0) filledCircle else emptyCircle)
        imgPinMask2.setImageResource(if (length > 1) filledCircle else emptyCircle)
        imgPinMask3.setImageResource(if (length > 2) filledCircle else emptyCircle)
        imgPinMask4.setImageResource(if (length > 3) filledCircle else emptyCircle)
        imgPinMask5.setImageResource(if (length > 4) filledCircle else emptyCircle)
        imgPinMask6.setImageResource(if (length > 5) filledCircle else emptyCircle)
    }
}
