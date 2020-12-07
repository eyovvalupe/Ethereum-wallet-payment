package io.horizontalsystems.bankwallet.modules.swap

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.fitSystemWindowsAndAdjustResize
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceViewModel
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveFragment
import io.horizontalsystems.bankwallet.modules.swap.coincard.SwapCoinCardViewModel
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationLiveData
import io.horizontalsystems.core.setOnSingleClickListener
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.fragment_swap.*

class SwapFragment : BaseFragment() {

    private val vmFactory by lazy { SwapModule.Factory(this, requireArguments().getParcelable(fromCoinKey)!!) }
    private val viewModel by navGraphViewModels<SwapViewModel>(R.id.swapFragment) { vmFactory }
    private val allowanceViewModel by navGraphViewModels<SwapAllowanceViewModel>(R.id.swapFragment) { vmFactory }
    private val feeViewModel by navGraphViewModels<EthereumFeeViewModel>(R.id.swapFragment) { vmFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_swap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootView.fitSystemWindowsAndAdjustResize()

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuCancel -> {
                    findNavController().popBackStack()
                    true
                }
                R.id.menuInfo -> {
                    findNavController().navigate(R.id.swapFragment_to_uniswapInfoFragment, null, navOptions())
                    true
                }
                else -> false
            }
        }

        val fromCoinCardViewModel = ViewModelProvider(this, vmFactory).get(SwapModule.Factory.coinCardTypeFrom, SwapCoinCardViewModel::class.java)
        val fromCoinCardTitle = getString(R.string.Swap_FromAmountTitle)
        fromCoinCard.initialize(fromCoinCardTitle, fromCoinCardViewModel, this, viewLifecycleOwner)

        val toCoinCardViewModel = ViewModelProvider(this, vmFactory).get(SwapModule.Factory.coinCardTypeTo, SwapCoinCardViewModel::class.java)
        val toCoinCardTile = getString(R.string.Swap_ToAmountTitle)
        toCoinCard.initialize(toCoinCardTile, toCoinCardViewModel, this, viewLifecycleOwner)

        allowanceView.initialize(allowanceViewModel, viewLifecycleOwner)

        observeViewModel()

        feeSelectorView.setDurationVisible(false)
        feeSelectorView.setFeeSelectorViewInteractions(feeViewModel, feeViewModel, viewLifecycleOwner, parentFragmentManager)

        getNavigationLiveData(SwapApproveFragment.requestKey)?.observe(viewLifecycleOwner, {
            if (it.getBoolean(SwapApproveFragment.resultKey)) {
                viewModel.didApprove()
            }
        })

        switchButton.setOnClickListener {
            viewModel.onTapSwitch()
        }

        advancedSettings.setOnSingleClickListener {
            findNavController().navigate(R.id.swapFragment_to_swapTradeOptionsFragment)
        }

        approveButton.setOnSingleClickListener {
            viewModel.onTapApprove()
        }

        proceedButton.setOnSingleClickListener {
            findNavController().navigate(R.id.swapFragment_to_swapConfirmationFragment, null, navOptions())
        }
    }

    private fun observeViewModel() {
        viewModel.isLoadingLiveData().observe(viewLifecycleOwner, { isLoading ->
            progressBar.isVisible = isLoading
        })

        viewModel.swapErrorLiveData().observe(viewLifecycleOwner, { error ->
            commonError.text = error
            commonError.isVisible = error != null
        })

        viewModel.tradeViewItemLiveData().observe(viewLifecycleOwner, { tradeViewItem ->
            setTradeViewItem(tradeViewItem)
        })

        viewModel.proceedActionLiveData().observe(viewLifecycleOwner, { action ->
            handleButtonAction(proceedButton, action)
        })

        viewModel.approveActionLiveData().observe(viewLifecycleOwner, { approveActionState ->
            handleButtonAction(approveButton, approveActionState)
        })

        viewModel.openApproveLiveEvent().observe(viewLifecycleOwner, { approveData ->
            findNavController().navigate(R.id.swapFragment_to_swapApproveFragment, bundleOf(SwapApproveFragment.dataKey to approveData), navOptions())
        })

        viewModel.advancedSettingsVisibleLiveData().observe(viewLifecycleOwner, { visible ->
            advancedSettingsViews.isVisible = visible
        })

        viewModel.feeVisibleLiveData().observe(viewLifecycleOwner, { visible ->
            feeSelectorView.isVisible = visible
        })
    }

    private fun handleButtonAction(button: Button, action: SwapViewModel.ActionState?) {
        when (action) {
            SwapViewModel.ActionState.Hidden -> {
                button.isVisible = false
            }
            is SwapViewModel.ActionState.Enabled -> {
                button.isVisible = true
                button.isEnabled = true
                button.text = action.title
            }
            is SwapViewModel.ActionState.Disabled -> {
                button.isVisible = true
                button.isEnabled = false
                button.text = action.title
            }
        }
    }

    private fun setTradeViewItem(tradeViewItem: SwapViewModel.TradeViewItem?) {
        price.text = tradeViewItem?.price

        if (tradeViewItem?.priceImpact != null) {
            priceImpactViews.isVisible = true
            priceImpactValue.text = tradeViewItem.priceImpact.value
            priceImpactValue.setTextColor(priceImpactColor(requireContext(), tradeViewItem.priceImpact.level))
        } else {
            priceImpactViews.isVisible = false
        }

        if (tradeViewItem?.guaranteedAmount != null) {
            guaranteedAmountViews.isVisible = true
            minMaxTitle.text = tradeViewItem.guaranteedAmount.title
            minMaxValue.text = tradeViewItem.guaranteedAmount.value
        } else {
            guaranteedAmountViews.isVisible = false
        }
    }

    private fun priceImpactColor(ctx: Context, priceImpactLevel: SwapTradeService.PriceImpactLevel?) =
            when (priceImpactLevel) {
                SwapTradeService.PriceImpactLevel.Normal -> LayoutHelper.getAttr(R.attr.ColorRemus, ctx.theme)
                        ?: ctx.getColor(R.color.green_d)
                SwapTradeService.PriceImpactLevel.Warning -> LayoutHelper.getAttr(R.attr.ColorJacob, ctx.theme)
                        ?: ctx.getColor(R.color.yellow_d)
                SwapTradeService.PriceImpactLevel.Forbidden -> LayoutHelper.getAttr(R.attr.ColorLucian, ctx.theme)
                        ?: ctx.getColor(R.color.red_d)
                else -> ctx.getColor(R.color.grey)
            }

    companion object {
        const val fromCoinKey = "fromCoinKey"
    }

}
