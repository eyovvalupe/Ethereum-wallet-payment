package io.horizontalsystems.bankwallet.modules.managewallets.view

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenFragment
import io.horizontalsystems.bankwallet.modules.addtoken.TokenType
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsModule
import io.horizontalsystems.bankwallet.modules.noaccount.NoAccountDialog
import io.horizontalsystems.bankwallet.modules.restore.RestoreFragment
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinListBaseFragment
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_manage_wallets.*

class ManageWalletsFragment : CoinListBaseFragment(), NoAccountDialog.Listener {

    override val title
        get() = getString(R.string.ManageCoins_title)

    private lateinit var viewModel: ManageWalletsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.inflateMenu(R.menu.manage_wallets_menu)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuAddToken -> {
                    hideKeyboard()
                    showAddTokenDialog()
                    true
                }
                else -> false
            }
        }
        configureSearchMenu(toolbar.menu, R.string.ManageCoins_Search)

        viewModel = ViewModelProvider(this, ManageWalletsModule.Factory())
                .get(ManageWalletsViewModel::class.java)

        activity?.onBackPressedDispatcher?.addCallback(this) {
            findNavController().popBackStack()
        }

        observe()
    }

    override fun onAttachFragment(childFragment: Fragment) {
        if (childFragment is NoAccountDialog) {
            childFragment.setListener(this)
        }
    }

    override fun searchExpanded(menu: Menu) {
        menu.findItem(R.id.menuAddToken)?.isVisible = false
    }

    override fun searchCollapsed(menu: Menu) {
        menu.findItem(R.id.menuAddToken)?.isVisible = true
    }

    // ManageWalletItemsAdapter.Listener

    override fun enable(coin: Coin) {
        viewModel.enable(coin)
    }

    override fun disable(coin: Coin) {
        viewModel.disable(coin)
    }

    override fun select(coin: Coin) {
        NoAccountDialog.show(childFragmentManager, coin)
    }

    // CoinListBaseFragment

    override fun updateFilter(query: String) {
        viewModel.updateFilter(query)
    }

    override fun onCancelAddressFormatSelection() {
        viewModel.onCancelDerivationSelection()
    }

    override fun onSelectAddressFormat(coin: Coin, derivationSetting: DerivationSetting) {
        viewModel.onSelect(coin, derivationSetting)
    }

    // NoAccountDialog.Listener

    override fun onClickRestoreKey(predefinedAccountType: PredefinedAccountType) {
        val arguments = Bundle(3).apply {
            putParcelable(RestoreFragment.PREDEFINED_ACCOUNT_TYPE_KEY, predefinedAccountType)
            putBoolean(RestoreFragment.SELECT_COINS_KEY, false)
            putBoolean(RestoreFragment.IN_APP_KEY, true)
        }

        findNavController().navigate(R.id.manageWalletsFragment_to_restoreFragment, arguments, navOptions())
    }

    private fun observe() {
        viewModel.viewStateLiveData.observe(viewLifecycleOwner, Observer { state ->
            setViewState(state)
        })

        viewModel.openDerivationSettingsLiveEvent.observe(viewLifecycleOwner, Observer { (coin, currentDerivation) ->
            hideKeyboard()
            showAddressFormatSelectionDialog(coin, currentDerivation)
        })
    }

    private fun showAddTokenDialog() {
        hideKeyboard()
        activity?.let {
            AddTokenDialog.show(it, object : AddTokenDialog.Listener {
                override fun onClickAddErc20Token() {
                    openAddToken(TokenType.Erc20)
                }

                override fun onClickAddBep2Token() {
                    openAddToken(TokenType.Bep2)
                }
            })
        }
    }

    private fun openAddToken(tokenType: TokenType) {
        val arguments = Bundle(1).apply {
            putParcelable(AddTokenFragment.TOKEN_TYPE_KEY, tokenType)
        }
        findNavController().navigate(R.id.manageWalletsFragment_to_addErc20Token, arguments, navOptions())
    }
}
