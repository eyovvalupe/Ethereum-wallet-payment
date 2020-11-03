package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CommunicationMode
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.modules.settings.security.privacy.PrivacySettingsModule.IPrivacySettingsViewDelegate
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.views.SettingsViewDropdown
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer

sealed class PrivacySettingsType {
    open val selectedTitle: String = ""

    class Communication(var selected: CommunicationMode) : PrivacySettingsType() {
        override val selectedTitle: String
            get() = selected.title
    }

    class WalletRestore(var selected: SyncMode) : PrivacySettingsType() {
        override val selectedTitle: String
            get() = selected.title
    }
}

data class PrivacySettingsViewItem(
        val coin: Coin,
        val settingType: PrivacySettingsType,
        var enabled: Boolean = true
)

class PrivacySettingsAdapter(
        private val delegate: IPrivacySettingsViewDelegate,
        private val title: String,
        private val description: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_ITEM = 1
    private val VIEW_TYPE_TITLE = 2
    private val VIEW_TYPE_DESCRIPTION = 3

    var items = listOf<PrivacySettingsViewItem>()

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> VIEW_TYPE_TITLE
            items.size + 1 -> VIEW_TYPE_DESCRIPTION
            else -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM -> {
                val settingsView = SettingsViewDropdown(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                PrivacySettingsItemViewHolder(settingsView, delegate)
            }
            VIEW_TYPE_TITLE -> TitleViewHolder.create(parent)
            VIEW_TYPE_DESCRIPTION -> DescriptionViewHolder.create(parent)
            else -> throw IllegalStateException("No such view type")
        }
    }

    override fun getItemCount(): Int {
        if (items.isEmpty()) {
            return 0
        }
        return items.size + 2
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TitleViewHolder -> holder.bind(title)
            is DescriptionViewHolder -> holder.bind(description)
            is PrivacySettingsItemViewHolder -> holder.bind(items[position - 1], position == items.size)
        }
    }

    class TitleViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(text: String) {
            containerView.findViewById<TextView>(R.id.titleText)?.text = text
        }

        companion object {
            const val layout = R.layout.view_holder_privacy_settings_section_title

            fun create(parent: ViewGroup) = TitleViewHolder(inflate(parent, layout, false))
        }

    }

    class DescriptionViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(text: String) {
            containerView.findViewById<TextView>(R.id.descriptionText)?.text = text
        }

        companion object {
            const val layout = R.layout.view_holder_privacy_settings_section_description

            fun create(parent: ViewGroup) = DescriptionViewHolder(inflate(parent, layout, false))
        }

    }

    class PrivacySettingsItemViewHolder(override val containerView: SettingsViewDropdown, private val viewDelegate: IPrivacySettingsViewDelegate)
        : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(viewItem: PrivacySettingsViewItem, lastElement: Boolean) {
            containerView.showIcon(AppLayoutHelper.getCoinDrawable(containerView.context, viewItem.coin.code, viewItem.coin.type))
            containerView.showTitle(viewItem.coin.title)
            containerView.showBottomBorder(lastElement)

            containerView.showDropdownValue(viewItem.settingType.selectedTitle)
            containerView.showDropdownIcon(viewItem.enabled)

            containerView.isEnabled = viewItem.enabled

            containerView.setOnClickListener {
                viewDelegate.didTapItem(viewItem.settingType, bindingAdapterPosition - 1)
            }
        }
    }
}

