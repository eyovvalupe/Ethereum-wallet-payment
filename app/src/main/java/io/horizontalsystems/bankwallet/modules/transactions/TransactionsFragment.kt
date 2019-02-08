package io.horizontalsystems.bankwallet.modules.transactions

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.annotation.NonNull
import android.support.design.widget.BottomSheetBehavior
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionInfoViewModel
import io.horizontalsystems.bankwallet.ui.extensions.NpaLinearLayoutManager
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_transactions.*
import kotlinx.android.synthetic.main.transaction_info_bottom_sheet.*
import kotlinx.android.synthetic.main.view_holder_filter.*
import kotlinx.android.synthetic.main.view_holder_transaction.*

class TransactionsFragment : android.support.v4.app.Fragment(), TransactionsAdapter.Listener, FilterAdapter.Listener {

    private lateinit var viewModel: TransactionsViewModel
    private lateinit var transInfoViewModel: TransactionInfoViewModel
    private val transactionsAdapter = TransactionsAdapter(this)
    private val filterAdapter = FilterAdapter(this)
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(TransactionsViewModel::class.java)
        viewModel.init()

        transactionsAdapter.viewModel = viewModel
        toolbar.setTitle(R.string.Transactions_Title)

        recyclerTransactions.setHasFixedSize(true)
        recyclerTransactions.adapter = transactionsAdapter
        recyclerTransactions.layoutManager = NpaLinearLayoutManager(context)
        recyclerTransactions.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                filterAdapter.filterChangeable = newState == SCROLL_STATE_IDLE
            }
        })

        recyclerTags.adapter = filterAdapter
        recyclerTags.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        viewModel.filterItems.observe(this, Observer { filters ->
            filters?.let {
                filterAdapter.setFilters(it)
            }
        })

        viewModel.transactionViewItemLiveEvent.observe(this, Observer { transactionViewItem ->
            transactionViewItem?.let {
                transInfoViewModel.setViewItem(it)
            }
        })

        viewModel.reloadLiveEvent.observe(this, Observer {
            transactionsAdapter.notifyDataSetChanged()
            if (transactionsAdapter.itemCount == 0) {
                viewModel.delegate.onBottomReached()
            }

            recyclerTransactions.visibility = if (viewModel.delegate.itemsCount == 0) View.GONE else View.VISIBLE
            emptyListText.visibility = if (viewModel.delegate.itemsCount == 0) View.VISIBLE else View.GONE
        })

        viewModel.addItemsLiveEvent.observe(this, Observer {
            it?.let { (fromIndex, count) ->
                transactionsAdapter.notifyItemRangeInserted(fromIndex, count)
            }
        })

        viewModel.reloadItemsLiveEvent.observe(this, Observer {
            it?.forEach { index ->
                transactionsAdapter.notifyItemChanged(index)
            }
        })

        setBottomSheet()
    }

    //Bottom sheet shows TransactionInfo
    private fun setBottomSheet() {

        bottomSheetBehavior = BottomSheetBehavior.from(nestedScrollView)

        transactionsDim.visibility = View.GONE
        transactionsDim.alpha = 0f

        var bottomSheetSlideOffOld = 0f

        bottomSheetBehavior?.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(@NonNull bottomSheet: View, newState: Int) {}

            override fun onSlide(@NonNull bottomSheet: View, slideOffset: Float) {
                transactionsDim.alpha = slideOffset
                if (bottomSheetSlideOffOld >= 0.7 && slideOffset < 0.7) {
                    (activity as? MainActivity)?.setBottomNavigationVisible(true)
                } else if (bottomSheetSlideOffOld >= 0.8 && slideOffset > 0.9) {
                    (activity as? MainActivity)?.setBottomNavigationVisible(false)
                }

                transactionsDim.visibility = if (slideOffset == 0f) View.GONE else View.VISIBLE

                bottomSheetSlideOffOld = slideOffset
            }
        })

        transInfoViewModel = ViewModelProviders.of(this).get(TransactionInfoViewModel::class.java)
        transInfoViewModel.init()

        transactionIdView.setOnClickListener { transInfoViewModel.onClickTransactionId() }
        txtFullInfo.setOnClickListener { transInfoViewModel.onClickOpenFillInfo() }
        transactionsDim.setOnClickListener { bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED }

        transInfoViewModel.showCopiedLiveEvent.observe(this, Observer {
            HudHelper.showSuccessMessage(R.string.Hud_Text_Copied)
        })

        transInfoViewModel.showFullInfoLiveEvent.observe(this, Observer { pair ->
            pair?.let {
                activity?.let { activity ->
                    FullTransactionInfoModule.start(activity, transactionHash = it.first, coinCode = it.second)
                }
            }
        })

        transInfoViewModel.transactionLiveData.observe(this, Observer { txRecord ->
            txRecord?.let { txRec ->
                (activity as? MainActivity)?.setBottomNavigationVisible(false)
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

                val txStatus = txRec.status

                fiatValue.apply {
                    text = txRec.currencyValue?.let { App.numberFormatter.format(it, showNegativeSign = true, realNumber = true) }
                    setTextColor(resources.getColor(if (txRec.incoming) R.color.green_crypto else R.color.yellow_crypto, null))
                }

                coinValue.apply {
                    text = App.numberFormatter.format(txRec.coinValue, true, true)
                }

                itemTime.apply {
                    bindTime(title = getString(R.string.TransactionInfo_Time), time = txRec.date?.let { DateHelper.getFullDateWithShortMonth(it) } ?: "")
                }

                itemStatus.apply {
                    bindStatus(txStatus)
                }

                transactionIdView.bindTransactionId(txRec.transactionHash)

                itemFrom.apply {
                    setOnClickListener { transInfoViewModel.onClickFrom() }
                    visibility = if (txRec.from.isNullOrEmpty()) View.GONE else View.VISIBLE
                    bindAddress(title = getString(R.string.TransactionInfo_From), address = txRec.from, showBottomBorder = true)
                }

                itemTo.apply {
                    setOnClickListener { transInfoViewModel.onClickTo() }
                    visibility = if (txRec.to.isNullOrEmpty()) View.GONE else View.VISIBLE
                    bindAddress(title = getString(R.string.TransactionInfo_To), address = txRec.to, showBottomBorder = true)
                }
            }
        })
    }

    override fun onItemClick(item: TransactionViewItem) {
        viewModel.delegate.onTransactionItemClick(item)
    }

    override fun onFilterItemClick(item: String?) {
        viewModel.delegate.onFilterSelect(item)
    }

    fun onBackPressed(): Boolean {
        if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            return true
        }
        return false
    }

}


class TransactionsAdapter(private var listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), ViewHolderTransaction.ClickListener {

    interface Listener {
        fun onItemClick(item: TransactionViewItem)
    }

    lateinit var viewModel: TransactionsViewModel

    override fun getItemCount(): Int {
        return viewModel.delegate.itemsCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolderTransaction(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_transaction, parent, false), this)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position > itemCount - 3) {
            viewModel.delegate.onBottomReached()
        }

        when (holder) {
            is ViewHolderTransaction -> {
                holder.bind(viewModel.delegate.itemForIndex(position))
            }
        }
    }

    override fun onClick(position: Int) {
        listener.onItemClick(viewModel.delegate.itemForIndex(position))
    }
}

class ViewHolderTransaction(override val containerView: View, private val l: ClickListener ) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    interface ClickListener {
        fun onClick(position: Int)
    }

    init {
        containerView.setOnSingleClickListener { l.onClick(adapterPosition) }
    }

    fun bind(transactionRecord: TransactionViewItem) {
        txValueInFiat.text = transactionRecord.currencyValue?.let { App.numberFormatter.formatForTransactions(it, transactionRecord.incoming) }
        txValueInCoin.text = App.numberFormatter.format(transactionRecord.coinValue, true)
        txDate.text = transactionRecord.date?.let { DateHelper.getShortDateForTransaction(it) }
        val time = transactionRecord.date?.let { DateHelper.getOnlyTime(it) }
        txStatusWithTimeView.bind(transactionRecord.status, time)
    }
}

class FilterAdapter(private var listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), ViewHolderFilter.ClickListener {

    interface Listener {
        fun onFilterItemClick(item: String?)
    }

    var filterChangeable = true

    private var selectedFilterId: String? = null
    private var filters: List<String?> = listOf()

    fun setFilters(filters: List<String?>) {
        this.filters = filters
        selectedFilterId = null
        notifyDataSetChanged()
    }

    override fun getItemCount() = filters.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolderFilter(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_filter, parent, false), this)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderFilter -> holder.bind(filters[position], selectedFilterId == filters[position])
        }
    }

    override fun onClickItem(position: Int) {
        if (filterChangeable) {
            listener.onFilterItemClick(filters[position])
            selectedFilterId = filters[position]
            notifyDataSetChanged()
        }
    }
}

class ViewHolderFilter(override val containerView: View, private val l: ClickListener) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    interface ClickListener {
        fun onClickItem(position: Int)
    }

    fun bind(filterName: String?, active: Boolean) {
        filter_text.text = filterName ?: containerView.context.getString(R.string.Transactions_FilterAll)
        filter_text.isActivated = active
        filter_text.setOnClickListener { l.onClickItem(adapterPosition) }
    }
}
