package io.horizontalsystems.bankwallet.modules.balance

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.managecoins.ManageCoinsModule
import io.horizontalsystems.bankwallet.modules.receive.ReceiveModule
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.ui.extensions.NpaLinearLayoutManager
import io.horizontalsystems.bankwallet.viewHelpers.AnimationHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.view_holder_add_coin.*
import kotlinx.android.synthetic.main.view_holder_coin.*
import java.math.BigDecimal

class BalanceFragment : android.support.v4.app.Fragment(), CoinsAdapter.Listener {

    private lateinit var viewModel: BalanceViewModel
    private var coinsAdapter = CoinsAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbar.setTitle(R.string.Balance_Title)

        viewModel = ViewModelProviders.of(this).get(BalanceViewModel::class.java)
        viewModel.init()

        viewModel.totalBalanceLiveData.observe(this, Observer { total ->
            ballanceText.text = total?.let { App.numberFormatter.format(it) } ?: ""
        })

        viewModel.openReceiveDialog.observe(this, Observer { adapterId ->
            adapterId?.let { id ->
                activity?.let {
                    ReceiveModule.start(it, id)
                }
            }
        })

        viewModel.openSendDialog.observe(this, Observer { iAdapter ->
            iAdapter?.let { coin ->
                activity?.let {
                    SendModule.start(it, coin)
                }
            }
        })

        viewModel.balanceColorLiveDate.observe(this, Observer { color ->
            color?.let { colorRes ->
                context?.let { it ->
                    ballanceText.setTextColor(ContextCompat.getColor(it, colorRes))
                }
            }
        })

        viewModel.didRefreshLiveEvent.observe(this, Observer {
            pullToRefresh.isRefreshing = false
        })

        viewModel.openManageCoinsLiveEvent.observe(this, Observer {
            context?.let { context -> ManageCoinsModule.start(context) }
        })

        viewModel.reloadLiveEvent.observe(this, Observer {
            coinsAdapter.notifyDataSetChanged()
            reloadHeader()
        })

        viewModel.reloadHeaderLiveEvent.observe(this, Observer {
            reloadHeader()
        })

        viewModel.reloadItemLiveEvent.observe(this, Observer { position ->
            position?.let {
                coinsAdapter.notifyItemChanged(it)
            }
        })


        coinsAdapter.viewDelegate = viewModel.delegate
        recyclerCoins.adapter = coinsAdapter
        recyclerCoins.layoutManager = NpaLinearLayoutManager(context)
        (recyclerCoins.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        pullToRefresh.setOnRefreshListener {
            viewModel.delegate.refresh()
        }
    }

    private fun reloadHeader() {
        val headerViewItem = viewModel.delegate.getHeaderViewItem()

        context?.let {
            val color = if (headerViewItem.upToDate) R.color.yellow_crypto else R.color.yellow_crypto_40
            ballanceText.setTextColor(ContextCompat.getColor(it, color))
        }

        ballanceText.text = headerViewItem.currencyValue?.let {
            App.numberFormatter.format(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerCoins.adapter = null
    }

    override fun onSendClicked(position: Int) {
        viewModel.onSendClicked(position)
    }

    override fun onReceiveClicked(position: Int) {
        viewModel.onReceiveClicked(position)
    }

    override fun onItemClick(position: Int) {
        coinsAdapter.toggleViewHolder(position)
    }

    override fun onAddCoinClick() {
        viewModel.delegate.openManageCoins()
    }
}

class CoinsAdapter(private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onSendClicked(position: Int)
        fun onReceiveClicked(position: Int)
        fun onItemClick(position: Int)
        fun onAddCoinClick()
    }

    private val coinType = 1
    private val addCoinType = 2

    private var expandedViewPosition: Int? = null

    lateinit var viewDelegate: BalanceModule.IViewDelegate

    fun toggleViewHolder(position: Int) {
        expandedViewPosition?.let {
            notifyItemChanged(it, false)
        }

        if (expandedViewPosition != position) {
            notifyItemChanged(position, true)
        }

        expandedViewPosition = if (expandedViewPosition == position) null else position
    }

    override fun getItemCount() = viewDelegate.itemsCount + 1

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1) addCoinType else coinType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                addCoinType -> ViewHolderAddCoin(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_add_coin, parent, false), listener)
                else -> ViewHolderCoin(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin, parent, false), listener)
            }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        when (holder) {
            is ViewHolderCoin -> holder.unbind()
        }
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {}

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (holder !is ViewHolderCoin) return

        if (payloads.isEmpty()) {
            holder.bind(viewDelegate.getViewItem(position), expandedViewPosition == position)
        } else if (payloads.any { it is Boolean }) {
            holder.bindPartial(expandedViewPosition == position)
        }
    }
}

class ViewHolderAddCoin(override val containerView: View, listener: CoinsAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    init {
        manageCoins.setOnSingleClickListener {
            listener.onAddCoinClick()
        }
    }
}

class ViewHolderCoin(override val containerView: View, private val listener: CoinsAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var disposable: Disposable? = null

    fun bind(balanceViewItem: BalanceViewItem, expanded: Boolean) {
        buttonPay.isEnabled = false
        imgSyncFailed.visibility = View.GONE
        textExchangeRate.visibility = View.GONE
        progressSync.visibility = View.GONE
        textSyncProgress.visibility = View.GONE

        textCoinAmount.text = App.numberFormatter.format(balanceViewItem.coinValue)
        balanceViewItem.currencyValue?.let {
            textCurrencyAmount.text = App.numberFormatter.format(it)
            textCurrencyAmount.visibility = View.VISIBLE
            textCurrencyAmount.setTextColor(ContextCompat.getColor(containerView.context, if (balanceViewItem.rateExpired) R.color.yellow_crypto_40 else R.color.yellow_crypto))
        } ?: run { textCurrencyAmount.visibility = View.GONE }

        balanceViewItem.state.let { adapterState ->
            when (adapterState) {
                is AdapterState.Syncing -> {
                    progressSync.visibility = View.VISIBLE
                    textSyncProgress.visibility = View.VISIBLE

                    disposable?.dispose()
                    disposable = adapterState.progressSubject
                            ?.observeOn(AndroidSchedulers.mainThread())
                            ?.subscribe {
                                val progress = (it * 100).toInt()
                                textSyncProgress.text = "$progress%"
                            }

                }
                is AdapterState.Synced -> {
                    if (balanceViewItem.coinValue.value > BigDecimal.ZERO) {
                        buttonPay.isEnabled = true
                    }

                    textExchangeRate.visibility = View.VISIBLE
                }
                is AdapterState.NotSynced -> {
                    imgSyncFailed.visibility = View.VISIBLE
                }
            }
        }

        coinIcon.bind(balanceViewItem.coin)
        textCoinName.text = balanceViewItem.coin.title

        textExchangeRate.text = balanceViewItem.exchangeValue?.let { exchangeValue ->
            containerView.context.getString(R.string.Balance_RatePerCoin, App.numberFormatter.format(exchangeValue), balanceViewItem.coinValue.coinCode)
        } ?: ""
        textExchangeRate.setTextColor(ContextCompat.getColor(containerView.context, if (balanceViewItem.rateExpired) R.color.steel_40 else R.color.grey))

        buttonPay.setOnSingleClickListener {
            listener.onSendClicked(adapterPosition)
        }

        buttonReceive.setOnSingleClickListener {
            listener.onReceiveClicked(adapterPosition)
        }

        viewHolderRoot.isSelected = expanded
        buttonsWrapper.visibility = if (expanded) View.VISIBLE else View.GONE
        containerView.setOnClickListener {
            listener.onItemClick(adapterPosition)
        }
    }

    fun bindPartial(expanded: Boolean) {
        viewHolderRoot.isSelected = expanded
        if (expanded) {
            AnimationHelper.expand(buttonsWrapper)
        } else {
            AnimationHelper.collapse(buttonsWrapper)
        }

    }

    fun unbind() {
        disposable?.dispose()
    }

}
