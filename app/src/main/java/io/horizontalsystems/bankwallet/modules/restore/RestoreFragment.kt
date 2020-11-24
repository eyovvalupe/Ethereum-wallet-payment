package io.horizontalsystems.bankwallet.modules.restore

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.restore.eos.RestoreEosFragment
import io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins.RestoreSelectCoinsFragment
import io.horizontalsystems.bankwallet.modules.restore.restoreselectpredefinedaccounttype.RestoreSelectPredefinedAccountTypeFragment
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsFragment
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsModule.RestoreAccountType
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_manage_keys.*

class RestoreFragment : BaseFragment() {

    private lateinit var viewModel: RestoreViewModel
    private var inApp = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_restore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectCoins = arguments?.getBoolean(SELECT_COINS_KEY)!!
        val predefinedAccountType = arguments?.getParcelable<PredefinedAccountType>(PREDEFINED_ACCOUNT_TYPE_KEY)
        inApp = arguments?.getBoolean(IN_APP_KEY) ?: true

        viewModel = ViewModelProvider(this, RestoreModule.Factory(selectCoins, predefinedAccountType))
                .get(RestoreViewModel::class.java)

        Handler().postDelayed({
            //without delay fragment is opened without slide animation
            openScreen(viewModel.initialScreen, addToStack = false)
        }, 10)

        observe()
    }

    private fun observe() {
        viewModel.openScreenLiveEvent.observe(viewLifecycleOwner, Observer {
            openScreen(it, addToStack = true)
        })

        viewModel.finishLiveEvent.observe(viewLifecycleOwner, Observer {
            closeWithSuccess()
        })
    }

    private fun closeWithSuccess() {
        if (inApp) {
            findNavController().popBackStack()
        } else {
            activity?.let {
                MainModule.start(it)
                it.finishAffinity()
            }
        }
    }

    private fun openScreen(screen: RestoreViewModel.Screen, addToStack: Boolean) {
        val fragment = getFragmentByScreen(screen)
        showFragment(fragment, addToStack)
    }

    private fun getFragmentByScreen(screen: RestoreViewModel.Screen): BaseFragment {
        return when (screen) {
            RestoreViewModel.Screen.SelectPredefinedAccountType -> {
                setPredefinedAccountTypeListener()

                RestoreSelectPredefinedAccountTypeFragment()
            }
            is RestoreViewModel.Screen.RestoreAccountType -> {
                setAccountTypeListener()

                when (screen.predefinedAccountType) {
                    PredefinedAccountType.Standard,
                    PredefinedAccountType.Binance,
                    PredefinedAccountType.Zcash -> {
                        val restoreAccountType = when (screen.predefinedAccountType) {
                            PredefinedAccountType.Standard -> RestoreAccountType.STANDARD
                            PredefinedAccountType.Binance -> RestoreAccountType.BINANCE
                            else -> RestoreAccountType.ZCASH
                        }
                        RestoreWordsFragment.instance(restoreAccountType, screen.predefinedAccountType.title)
                    }
                    PredefinedAccountType.Eos -> {
                        RestoreEosFragment()
                    }
                }
            }
            is RestoreViewModel.Screen.SelectCoins -> {
                setSelectCoinsListener()

                RestoreSelectCoinsFragment.instance(screen.predefinedAccountType)
            }
        }
    }

    private fun showFragment(fragment: BaseFragment, addToStack: Boolean) {
        childFragmentManager.commit {
            add(R.id.fragmentContainerView, fragment)
            if (addToStack) {
                addToBackStack(null)
            }
        }
    }

    private fun setPredefinedAccountTypeListener() {
        childFragmentManager.setFragmentResultListener(selectPredefinedAccountTypeRequestKey, viewLifecycleOwner, FragmentResultListener { requestKey, result ->
            val predefinedAccountType = result.getParcelable<PredefinedAccountType>(predefinedAccountTypeBundleKey) ?: return@FragmentResultListener
            viewModel.onSelect(predefinedAccountType)
        })
    }

    private fun setAccountTypeListener() {
        childFragmentManager.setFragmentResultListener(accountTypeRequestKey, viewLifecycleOwner, FragmentResultListener { requestKey, result ->
            val accountType = result.getParcelable<AccountType>(accountTypeBundleKey) ?: return@FragmentResultListener
            viewModel.onEnter(accountType)
        })
    }

    private fun setSelectCoinsListener() {
        childFragmentManager.setFragmentResultListener(selectCoinsRequestKey, viewLifecycleOwner, FragmentResultListener { requestKey, result ->
            val selectedCoins = result.getParcelableArrayList<Coin>(selectCoinsBundleKey) ?: return@FragmentResultListener
            viewModel.onSelect(selectedCoins)
        })
    }


    companion object {
        const val selectPredefinedAccountTypeRequestKey = "selectPredefinedAccountTypeRequestKey"
        const val predefinedAccountTypeBundleKey = "predefinedAccountTypeBundleKey"
        const val accountTypeRequestKey = "accountTypeRequestKey"
        const val accountTypeBundleKey = "accountTypeBundleKey"
        const val selectCoinsRequestKey = "selectCoinsRequestKey"
        const val selectCoinsBundleKey = "selectCoinsBundleKey"

        const val PREDEFINED_ACCOUNT_TYPE_KEY = "predefined_account_type_key"
        const val SELECT_COINS_KEY = "select_coins_key"
        const val IN_APP_KEY = "in_app_key"
    }
}
