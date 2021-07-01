package io.horizontalsystems.bankwallet.modules.settings.faq

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.Faq
import io.horizontalsystems.bankwallet.modules.markdown.MarkdownFragment
import io.horizontalsystems.bankwallet.modules.settings.guides.ErrorAdapter
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_faq_list.*
import kotlinx.android.synthetic.main.view_holder_faq_item.*
import kotlinx.android.synthetic.main.view_holder_faq_section.*

class FaqListFragment : BaseFragment(), FaqListAdapter.Listener {

    private val viewModel by viewModels<FaqViewModel> { FaqModule.Factory() }
    private val adapter = FaqListAdapter(this)
    private val errorAdapter = ErrorAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_faq_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        faqListRecyclerview.adapter = ConcatAdapter(errorAdapter, adapter)

        observeLiveData()
    }

    private fun observeLiveData() {
        viewModel.faqItemList.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })

        viewModel.loading.observe(viewLifecycleOwner, Observer {
            toolbarSpinner.isVisible = it
        })

        viewModel.error.observe(viewLifecycleOwner, Observer {
            errorAdapter.error = it
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()

        faqListRecyclerview.adapter = null
    }

    override fun onItemClicked(faqItem: FaqItem) {
        val arguments = bundleOf(MarkdownFragment.markdownUrlKey to faqItem.faq.markdown)
        findNavController().navigate(R.id.faqFragment_to_markdownFragment, arguments, navOptions())
    }
}

open class FaqData
data class FaqSection(val title: String) : FaqData()
data class FaqItem(val faq: Faq, var listPosition: ListPosition) : FaqData()

class FaqListAdapter(private val listener: Listener) : ListAdapter<FaqData, RecyclerView.ViewHolder>(faqDiff) {

    interface Listener {
        fun onItemClicked(faqItem: FaqItem)
    }

    private val viewTypeSection = 0
    private val viewTypeFaq = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
        viewTypeSection -> ViewHolderSection(inflate(parent, R.layout.view_holder_faq_section))
        else -> ViewHolderFaq(inflate(parent, R.layout.view_holder_faq_item), listener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)

        if (holder is ViewHolderSection && item is FaqSection) {
            holder.bind(item)
        }
        if (holder is ViewHolderFaq && item is FaqItem) {
            holder.bind(item)
        }
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is FaqSection -> viewTypeSection
        else -> viewTypeFaq
    }

    companion object {
        private val faqDiff = object : DiffUtil.ItemCallback<FaqData>() {
            override fun areItemsTheSame(oldItem: FaqData, newItem: FaqData): Boolean {
                return oldItem.equals(newItem)
            }

            override fun areContentsTheSame(oldItem: FaqData, newItem: FaqData): Boolean {
                return oldItem.equals(newItem)
            }
        }
    }
}

class ViewHolderSection(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun bind(item: FaqSection) {
        faqHeadText.text = item.title
    }
}

class ViewHolderFaq(override val containerView: View, listener: FaqListAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    private var faqItem: FaqItem? = null

    init {
        containerView.setOnClickListener {
            faqItem?.let {
                listener.onItemClicked(it)
            }
        }
    }

    fun bind(item: FaqItem) {
        faqItem = item
        faqTitleText.text = item.faq.title
        containerView.setBackgroundResource(item.listPosition.getBackground())
    }
}
