package io.horizontalsystems.bankwallet.modules.swapxxx

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swapxxx.providers.ISwapXxxProvider
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SwapViewModel(
    private val quoteService: SwapQuoteService,
    private val balanceService: TokenBalanceService,
    private val priceImpactService: PriceImpactService,
    private val quoteExpirationService: SwapQuoteExpirationService
) : ViewModelUiState<SwapUiState>() {

    private var quoteState = quoteService.stateFlow.value
    private var availableBalance = balanceService.balanceFlow.value
    private var priceImpactState = priceImpactService.stateFlow.value

    init {
        viewModelScope.launch {
            quoteService.stateFlow.collect {
                handleUpdatedQuoteState(it)
            }
        }
        viewModelScope.launch {
            balanceService.balanceFlow.collect {
                handleUpdatedBalance(it)
            }
        }
        viewModelScope.launch {
            priceImpactService.stateFlow.collect {
                handleUpdatedPriceImpactState(it)
            }
        }
        viewModelScope.launch {
            quoteExpirationService.quoteExpiredFlow.collect {
                handleUpdatedQuoteExpiredState(it)
            }
        }
    }

    override fun createState() = SwapUiState(
        amountIn = quoteState.amountIn,
        tokenIn = quoteState.tokenIn,
        tokenOut = quoteState.tokenOut,
        quoting = quoteState.quoting,
        swapEnabled = isSwapEnabled(),
        quotes = quoteState.quotes,
        preferredProvider = quoteState.preferredProvider,
        quoteLifetime = quoteState.quoteLifetime,
        quote = quoteState.quote,
        error = quoteState.error,
        availableBalance = availableBalance,
        priceImpact = priceImpactState.priceImpact,
        priceImpactLevel = priceImpactState.priceImpactLevel,
        priceImpactCaution = priceImpactState.priceImpactCaution
    )

    private fun handleUpdatedBalance(balance: BigDecimal?) {
        this.availableBalance = balance

        emitState()
    }

    private fun handleUpdatedQuoteState(quoteState: SwapQuoteService.State) {
        this.quoteState = quoteState

        balanceService.setToken(quoteState.tokenIn)
        priceImpactService.setPriceImpact(quoteState.quote?.priceImpact, quoteState.quote?.provider?.title)
        quoteExpirationService.setQuote(quoteState.quote)

        emitState()
    }

    private fun handleUpdatedQuoteExpiredState(quoteExpired: Boolean) {
        if (quoteExpired) {
            quoteService.reQuote()
        }
    }

    private fun handleUpdatedPriceImpactState(priceImpactState: PriceImpactService.State) {
        this.priceImpactState = priceImpactState

        emitState()
    }

    fun onSelectQuote(quote: SwapProviderQuote) {
        quoteService.selectQuote(quote)
    }

    private fun isSwapEnabled() = quoteState.quote != null

    fun onEnterAmount(v: BigDecimal?) = quoteService.setAmount(v)
    fun onSelectTokenIn(token: Token) = quoteService.setTokenIn(token)
    fun onSelectTokenOut(token: Token) = quoteService.setTokenOut(token)
    fun onSwitchPairs() = quoteService.switchPairs()
    fun onUpdateSettings(settings: Map<String, Any?>) = quoteService.setSwapSettings(settings)

    fun getCurrentQuote() = quoteState.quote
    fun getSettings() = quoteService.getSwapSettings()

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val swapQuoteService = SwapQuoteService(SwapProvidersManager())
            val tokenBalanceService = TokenBalanceService(App.adapterManager)
            val priceImpactService = PriceImpactService()

            return SwapViewModel(
                swapQuoteService,
                tokenBalanceService,
                priceImpactService,
                SwapQuoteExpirationService()
            ) as T
        }
    }
}

data class SwapUiState(
    val amountIn: BigDecimal?,
    val tokenIn: Token?,
    val tokenOut: Token?,
    val quoting: Boolean,
    val swapEnabled: Boolean,
    val quotes: List<SwapProviderQuote>,
    val preferredProvider: ISwapXxxProvider?,
    val quoteLifetime: Long,
    val quote: SwapProviderQuote?,
    val error: Throwable?,
    val availableBalance: BigDecimal?,
    val priceImpact: BigDecimal?,
    val priceImpactLevel: SwapMainModule.PriceImpactLevel?,
    val priceImpactCaution: HSCaution?
)
