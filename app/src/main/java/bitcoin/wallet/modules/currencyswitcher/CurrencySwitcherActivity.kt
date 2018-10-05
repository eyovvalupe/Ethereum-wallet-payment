package bitcoin.wallet.modules.currencyswitcher

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import bitcoin.wallet.BaseActivity
import bitcoin.wallet.R
import bitcoin.wallet.core.setOnSingleClickListener
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.ui.view.ViewHolderProgressbar
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_currency_switcher.*
import kotlinx.android.synthetic.main.view_holder_currency_item.*

class CurrencySwitcherActivity: BaseActivity(), CurrencySwitcherAdapter.Listener {

    private lateinit var viewModel: CurrencySwitcherViewModel
    private var adapter: CurrencySwitcherAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(CurrencySwitcherViewModel::class.java)
        viewModel.init()

        setContentView(R.layout.activity_currency_switcher)

        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.settings_currency_switcher_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.back)

        adapter = CurrencySwitcherAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.currencyItems.observe(this, Observer { items ->
            items?.let {
                adapter?.items = it
                adapter?.notifyDataSetChanged()
            }
        })
    }

    override fun onItemClick(item: Currency) {
        viewModel.delegate.onItemClick(item)
    }
}

class CurrencySwitcherAdapter(private var listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val VIEW_TYPE_ITEM = 1
    private val VIEW_TYPE_LOADING = 2

    interface Listener {
        fun onItemClick(item: Currency)
    }

    var items = listOf<CurrencyViewItem>()

    override fun getItemCount() = if (items.isEmpty()) 1 else items.size

    override fun getItemViewType(position: Int): Int = if(items.isEmpty()) {
        VIEW_TYPE_LOADING
    } else {
        VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ITEM -> ViewHolderCurrency(inflater.inflate(ViewHolderCurrency.layoutResourceId, parent, false))
            else -> ViewHolderProgressbar(inflater.inflate(ViewHolderProgressbar.layoutResourceId, parent, false))
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderCurrency -> holder.bind(items[position]) { listener.onItemClick(items[position].currency) }
        }
    }

}

class ViewHolderCurrency(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: CurrencyViewItem, onClick: () -> (Unit)) {

        containerView.setOnSingleClickListener { onClick.invoke() }
        title.text = item.currency.code
        subtitle.text = item.currency.name
        checkmarkIcon.visibility = if (item.selected) View.VISIBLE else View.GONE
    }

    companion object {
        val layoutResourceId: Int
            get() = R.layout.view_holder_currency_item
    }

}
