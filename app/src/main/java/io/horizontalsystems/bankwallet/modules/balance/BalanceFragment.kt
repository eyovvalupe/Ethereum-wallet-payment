package io.horizontalsystems.bankwallet.modules.balance

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.SimpleItemAnimator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.backup.BackupModule
import io.horizontalsystems.bankwallet.modules.balance.views.SyncErrorDialog
import io.horizontalsystems.bankwallet.modules.contact.ContactModule
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.managecoins.ManageWalletsModule
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartActivity
import io.horizontalsystems.bankwallet.modules.receive.ReceiveFragment
import io.horizontalsystems.bankwallet.modules.settings.managekeys.views.ManageKeysDialog
import io.horizontalsystems.bankwallet.modules.settings.security.privacy.PrivacySettingsModule
import io.horizontalsystems.bankwallet.ui.extensions.NpaLinearLayoutManager
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.core.getValueAnimator
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.measureHeight
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.fragment_balance.*

class BalanceFragment : Fragment(), BalanceItemsAdapter.Listener, ReceiveFragment.Listener {

    private lateinit var viewModel: BalanceViewModel
    private val balanceItemsAdapter = BalanceItemsAdapter(this)
    private var totalBalanceTabHeight: Int = 0
    private val animationPlaybackSpeed: Double = 1.3
    private val expandDuration: Long get() = (300L / animationPlaybackSpeed).toLong()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_balance, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(BalanceViewModel::class.java)
        viewModel.init()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        totalBalanceTabHeight = balanceTabWrapper.measureHeight()

        recyclerCoins.adapter = balanceItemsAdapter
        recyclerCoins.layoutManager = NpaLinearLayoutManager(context)
        (recyclerCoins.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        sortButton.setOnClickListener {
            viewModel.delegate.onSortClick()
        }

        pullToRefresh.setOnRefreshListener {
            viewModel.delegate.onRefresh()
        }

        totalBalanceWrapper.setOnClickListener { viewModel.delegate.onHideBalanceClick() }

        observeLiveData()
        setSwipeBackground()
    }

    private fun setOptionsMenuVisible(visible: Boolean) {
        if (visible) {
            toolbar.menu.clear()
            toolbar.inflateMenu(R.menu.balance_menu)
            toolbar.setOnMenuItemClickListener { item: MenuItem? ->
                if (item?.itemId == R.id.menuShowBalance) {
                    viewModel.delegate.onShowBalanceClick()
                    return@setOnMenuItemClickListener true
                }
                return@setOnMenuItemClickListener false
            }
        } else {
            toolbar.menu.clear()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerCoins.adapter = null
    }

    private fun setSwipeBackground() {
        activity?.theme?.let { theme ->
            LayoutHelper.getAttr(R.attr.SwipeRefreshBackgroundColor, theme)?.let { color ->
                pullToRefresh.setProgressBackgroundColorSchemeColor(color)
            }
            LayoutHelper.getAttr(R.attr.ColorOz, theme)?.let { color ->
                pullToRefresh.setColorSchemeColors(color)
            }
        }
    }

    // ReceiveFragment listener
    override fun shareReceiveAddress(address: String) {
        activity?.let {
            ShareCompat.IntentBuilder
                .from(it)
                .setType("text/plain")
                .setText(address)
                .startChooser()
        }
    }

    // BalanceAdapter listener

    override fun onSendClicked(viewItem: BalanceViewItem) {
        viewModel.delegate.onPay(viewItem)
    }

    override fun onReceiveClicked(viewItem: BalanceViewItem) {
        viewModel.delegate.onReceive(viewItem)
    }

    override fun onItemClicked(viewItem: BalanceViewItem) {
        viewModel.delegate.onItem(viewItem)
    }

    override fun onChartClicked(viewItem: BalanceViewItem) {
        viewModel.delegate.onChart(viewItem)
    }

    override fun onSyncErrorClicked(viewItem: BalanceViewItem) {
        viewModel.delegate.onSyncErrorClick(viewItem)
    }

    override fun onAddCoinClicked() {
        viewModel.delegate.onAddCoinClick()
    }

    override fun onAttachFragment(childFragment: Fragment) {
        if (childFragment is ReceiveFragment) {
            childFragment.setListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.delegate.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.delegate.onPause()
    }
    // LiveData

    private fun observeLiveData() {
        viewModel.openReceiveDialog.observe(viewLifecycleOwner, Observer { wallet ->
            ReceiveFragment.newInstance(wallet).show(childFragmentManager, "ReceiveFragment")
        })

        viewModel.openSendDialog.observe(viewLifecycleOwner, Observer {
            (activity as? MainActivity)?.openSend(it)
        })

        viewModel.didRefreshLiveEvent.observe(viewLifecycleOwner, Observer {
            Handler().postDelayed({ pullToRefresh.isRefreshing = false }, 1000)
        })

        viewModel.openManageCoinsLiveEvent.observe(viewLifecycleOwner, Observer {
            context?.let { ManageWalletsModule.start(it, true) }
        })

        viewModel.setViewItems.observe(viewLifecycleOwner, Observer {
            balanceItemsAdapter.submitList(it)
        })

        viewModel.setHeaderViewItem.observe(viewLifecycleOwner, Observer {
            setHeaderViewItem(it)
        })

        viewModel.openSortingTypeDialogLiveEvent.observe(viewLifecycleOwner, Observer { selected ->
            val sortTypes = listOf(BalanceSortType.Name, BalanceSortType.Value, BalanceSortType.PercentGrowth)
            val selectorItems = sortTypes.map {
                SelectorItem(getString(it.getTitleRes()), it == selected)
            }
            SelectorDialog
                    .newInstance(selectorItems, getString(R.string.Balance_Sort_PopupTitle), { position ->
                        viewModel.delegate.onSortTypeChange(sortTypes[position])
                    }, false)
                    .show(parentFragmentManager, "balance_sort_type_selector")
        })

        viewModel.isSortOn.observe(viewLifecycleOwner, Observer { visible ->
            sortButton.visibility = if (visible) View.VISIBLE else View.GONE
        })

        viewModel.showBackupAlert.observe(viewLifecycleOwner, Observer { (coin, predefinedAccount) ->
            activity?.let { activity ->
                val title = getString(R.string.ManageKeys_Delete_Alert_Title)
                val subtitle = getString(predefinedAccount.title)
                val description = getString(R.string.Balance_Backup_Alert, getString(predefinedAccount.title), coin.title)
                ManageKeysDialog.show(title, subtitle, description, activity, object : ManageKeysDialog.Listener {
                    override fun onClickBackupKey() {
                        viewModel.delegate.onBackupClick()
                    }
                }, ManageKeysDialog.ManageAction.BACKUP)
            }
        })

        viewModel.openBackup.observe(viewLifecycleOwner, Observer { (account, coinCodesStringRes) ->
            context?.let { BackupModule.start(it, account, getString(coinCodesStringRes)) }
        })

        viewModel.openChartModule.observe(viewLifecycleOwner, Observer { coin ->
            startActivity(Intent(activity, RateChartActivity::class.java).apply {
                putExtra(ModuleField.COIN_CODE, coin.code)
                putExtra(ModuleField.COIN_TITLE, coin.title)
            })
        })

        viewModel.openContactPage.observe(viewLifecycleOwner, Observer {
            activity?.let {
                ContactModule.start(it)
            }
        })

        viewModel.setBalanceHiddenLiveEvent.observe(viewLifecycleOwner, Observer { (hideBalance, animate) ->
            setOptionsMenuVisible(hideBalance)

            if (animate) {
                val animator = getValueAnimator(!hideBalance, expandDuration, AccelerateDecelerateInterpolator()
                ) { progress -> setExpandProgress(balanceTabWrapper, 0, totalBalanceTabHeight, progress) }
                animator.start()
            } else {
                setExpandProgress(balanceTabWrapper, 0, totalBalanceTabHeight, if (hideBalance) 0f else 1f)
            }
        })

        viewModel.showSyncError.observe(viewLifecycleOwner, Observer { (wallet, errorMessage, sourceChangeable) ->
            activity?.let{ fragmentActivity ->
                SyncErrorDialog.show(fragmentActivity, wallet.coin.title, sourceChangeable, object : SyncErrorDialog.Listener{
                    override fun onClickRetry() {
                        viewModel.delegate.refreshByWallet(wallet)
                    }

                    override fun onClickChangeSource() {
                        PrivacySettingsModule.start(fragmentActivity)
                    }

                    override fun onClickReport() {
                        viewModel.delegate.onReportClick(errorMessage)
                    }

                })
            }
        })

        viewModel.networkNotAvailable.observe(viewLifecycleOwner, Observer {
            HudHelper.showErrorMessage(this.requireView(), R.string.Hud_Text_NoInternet)
        })

        viewModel.showErrorMessageCopied.observe(viewLifecycleOwner, Observer {
            HudHelper.showSuccessMessage(this.requireView(), R.string.Hud_Text_Copied)
        })
    }

    private fun setExpandProgress(view: View, smallHeight: Int, bigHeight: Int, progress: Float) {
        view.layoutParams.height = (smallHeight + (bigHeight - smallHeight) * progress).toInt()
        view.requestLayout()
    }

    private fun setHeaderViewItem(headerViewItem: BalanceHeaderViewItem) {
        headerViewItem.apply {
            balanceText.text = xBalanceText
            context?.let { context -> balanceText.setTextColor(getBalanceTextColor(context)) }
        }
    }
}
