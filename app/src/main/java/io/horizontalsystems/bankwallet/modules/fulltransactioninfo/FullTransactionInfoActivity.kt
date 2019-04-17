package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.FullTransactionItem
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.dataprovider.DataProviderSettingsModule
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_full_transaction_info.*
import kotlinx.android.synthetic.main.view_holder_full_transaction.*
import kotlinx.android.synthetic.main.view_holder_full_transaction_item.*
import kotlinx.android.synthetic.main.view_holder_full_transaction_link.*
import kotlinx.android.synthetic.main.view_holder_full_transaction_source.*

class FullTransactionInfoActivity : BaseActivity(), FullTransactionInfoErrorFragment.Listener {

    private val transactionRecordAdapter = SectionViewAdapter(this)
    private lateinit var viewModel: FullTransactionInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transactionHash = intent.extras.getString(transactionHashKey)
        val coin = intent.extras.getSerializable(coinKey)

        viewModel = ViewModelProviders.of(this).get(FullTransactionInfoViewModel::class.java)
        viewModel.init(transactionHash, coin as Coin)

        setContentView(R.layout.activity_full_transaction_info)

        transactionHash?.let { transactionIdView.bindTransactionId(it) }

        shadowlessToolbar.bind(
                title = getString(R.string.FullInfo_Title),
                leftBtnItem = TopMenuItem(R.drawable.back) { onBackPressed() }
        )

        closeBtn.setOnClickListener { onBackPressed() }
        shareBtn.setOnClickListener { viewModel.share() }

        //
        // LiveData
        //
        viewModel.reloadLiveEvent.observe(this, Observer {
            recyclerTransactionInfo.visibility = View.VISIBLE
            transactionRecordAdapter.notifyDataSetChanged()
        })

        viewModel.loadingLiveData.observe(this, Observer { coinCode ->
            if (coinCode == true) {
                progressLoading.visibility = View.VISIBLE
                recyclerTransactionInfo.visibility = View.INVISIBLE
                transactionRecordAdapter.notifyDataSetChanged()
            } else {
                progressLoading.visibility = View.INVISIBLE
            }
        })

        viewModel.showCopiedLiveEvent.observe(this, Observer {
            HudHelper.showSuccessMessage(R.string.Hud_Text_Copied)
        })

        viewModel.openLinkLiveEvent.observe(this, Observer { url ->
            url?.let {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            }
        })

        viewModel.openProviderSettingsEvent.observe(this, Observer { data ->
            data?.let { (coin, transactionHash) ->
                DataProviderSettingsModule.start(this, coin, transactionHash)
            }
        })

        viewModel.showErrorLiveEvent.observe(this, Observer { error ->
            error?.let { (show, providerName) ->
                if (show && providerName != null) {
                    errorContainer.visibility = View.VISIBLE

                    val fragment = FullTransactionInfoErrorFragment.newInstance(providerName)
                    val transaction = supportFragmentManager.beginTransaction()

                    transaction.replace(R.id.errorContainer, fragment)
                    transaction.commit()
                } else {
                    errorContainer.visibility = View.INVISIBLE
                }
            }
        })

        viewModel.showShareLiveEvent.observe(this, Observer { url ->
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, url)
                type = "text/plain"
            }
            startActivity(sendIntent)
        })

        recyclerTransactionInfo.hasFixedSize()
        recyclerTransactionInfo.adapter = transactionRecordAdapter
        recyclerTransactionInfo.layoutManager = LinearLayoutManager(this)

        transactionRecordAdapter.viewModel = viewModel
    }

    //
    // FullTransactionInfoErrorFragment Listener
    //
    override fun onRetry() {
        viewModel.retry()
    }

    override fun onChangeProvider() {
        viewModel.changeProvider()
    }

    companion object {
        const val transactionHashKey = "transaction_hash"
        const val coinKey = "coin"

        fun start(context: Context, transactionHash: String, coin: Coin) {
            val intents = Intent(context, FullTransactionInfoActivity::class.java)
            intents.putExtra(transactionHashKey, transactionHash)
            intents.putExtra(coinKey, coin)
            context.startActivity(intents)
        }
    }
}

class SectionViewAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    lateinit var viewModel: FullTransactionInfoViewModel

    private val sectionViewSource = 0
    private val sectionView = 1
    private val sectionViewLink = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)

        return if (viewType == sectionViewSource) {
            SectionSourceViewHolder(view.inflate(R.layout.view_holder_full_transaction_source, parent, false))
        } else if (viewType == sectionView) {
            SectionViewHolder(view.inflate(R.layout.view_holder_full_transaction, parent, false))
        } else {
            SectionLinkViewHolder(view.inflate(R.layout.view_holder_full_transaction_link, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            sectionViewSource
        } else if (position == itemCount - 1) {
            sectionViewLink
        } else {
            sectionView
        }
    }

    override fun getItemCount(): Int {
        return viewModel.delegate.sectionCount + 2
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val providerName = viewModel.delegate.providerName

        when (holder) {
            is SectionSourceViewHolder -> {
                holder.transactionSource.bind(title = context.getString(R.string.FullInfo_Source), value = providerName, dimmed = false, icon = null)
                holder.transactionSource.setOnClickListener {
                    viewModel.delegate.onTapProvider()
                }
            }
            is SectionViewHolder -> {
                val posWithoutSource = position - 1

                viewModel.delegate.getSection(posWithoutSource)?.let { section ->
                    holder.sectionRecyclerView.hasFixedSize()
                    holder.sectionRecyclerView.isNestedScrollingEnabled = false

                    holder.sectionRecyclerView.layoutManager = LinearLayoutManager(context)
                    holder.sectionRecyclerView.adapter = SectionItemViewAdapter(context, viewModel, section.items)
                }

            }
            is SectionLinkViewHolder -> {
                providerName?.let {
                    val changeProviderStyle = SpannableString(providerName)
                    changeProviderStyle.setSpan(UnderlineSpan(), 0, changeProviderStyle.length, 0)

                    holder.transactionLink.text = changeProviderStyle
                    holder.transactionLink.setOnClickListener {
                        viewModel.delegate.onTapResource()
                    }
                }
            }
        }
    }
}

class SectionItemViewAdapter(val context: Context, val viewModel: FullTransactionInfoViewModel, val items: List<FullTransactionItem>)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.view_holder_full_transaction_item, parent, false)

        return SectionItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SectionItemViewHolder -> {
                val item = items[position]
                val notLast = items.size != position + 1

                bindTransaction(item, notLast, holder.sectionItem)
            }
        }
    }

    private fun bindTransaction(item: FullTransactionItem, showBorder: Boolean, viewItem: FullTransactionInfoItemView) {
        val title = if (item.titleResId != null) context.getString(item.titleResId) else item.title

        viewItem.bind(title, item.value, item.icon, item.dimmed, showBorder)

        if (item.clickable) {
            viewItem.setOnClickListener {
                viewModel.delegate.onTapItem(item)
            }
        }
    }
}

class SectionViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class SectionSourceViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class SectionLinkViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class SectionItemViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
