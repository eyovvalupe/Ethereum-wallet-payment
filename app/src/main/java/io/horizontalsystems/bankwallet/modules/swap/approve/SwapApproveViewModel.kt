package io.horizontalsystems.bankwallet.modules.swap.approve

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ethereum.CoinService
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal
import kotlin.math.min

class SwapApproveViewModel(
        private val service: ISwapApproveService,
        private val coinService: CoinService,
        private val ethCoinService: CoinService,
        private val clearables: List<Clearable>
) : ViewModel() {

    private val maxCoinDecimal = 8
    var amount: String
        get() {
            return service.amount?.let {
                coinService.convertToMonetaryValue(it).toPlainString()
            } ?: ""
        }
        set(value) {
            service.amount = when {
                value.isEmpty() -> null
                else -> coinService.convertToFractionalMonetaryValue(BigDecimal(value))
            }
        }

    private val disposables = CompositeDisposable()

    val approveAllowed = MutableLiveData<Boolean>()
    val approveSuccessLiveEvent = SingleLiveEvent<Unit>()
    val approveError = MutableLiveData<String>()
    val amountError = MutableLiveData<String?>(null)
    val error = MutableLiveData<String?>(null)

    init {
        service.stateObservable
                .subscribe {
                    handle(it)
                }
                .let {
                    disposables.add(it)
                }
    }

    fun validateAmount(value: String): Boolean {
        if (value.isEmpty()) return true

        return try {
            BigDecimal(value).scale() <= min(coinService.coin.decimal, maxCoinDecimal)
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun handle(approveState: SwapApproveService.State) {
        approveAllowed.postValue(approveState is SwapApproveService.State.ApproveAllowed)

        when (approveState) {
            SwapApproveService.State.Success -> {
                approveSuccessLiveEvent.postValue(Unit)
            }
            is SwapApproveService.State.Error -> {
                approveError.postValue(convertError(approveState.e))
            }
            is SwapApproveService.State.ApproveNotAllowed -> {
                val errors = approveState.errors.toMutableList()

                val balanceErrorIndex = errors.indexOfFirst {
                    it is SwapApproveService.TransactionAmountError
                }

                if (balanceErrorIndex != -1) {
                    amountError.postValue(convertError(errors.removeAt(balanceErrorIndex)))
                } else {
                    amountError.postValue(null)
                }

                error.postValue(errors.firstOrNull()?.let { convertError(it) })
            }
        }
    }

    private fun convertError(throwable: Throwable): String {
        return when (throwable) {
            is SwapApproveService.TransactionAmountError.AlreadyApproved -> {
                App.instance.getString(R.string.Approve_Error_AlreadyApproved)
            }
            is SwapApproveService.TransactionEthereumAmountError.InsufficientBalance -> {
                App.instance.getString(R.string.EthereumTransaction_Error_InsufficientBalance, ethCoinService.coinValue(throwable.requiredBalance))
            }
            else -> throwable.message ?: throwable.javaClass.simpleName
        }
    }

    fun onApprove() {
        service.approve()
    }

    override fun onCleared() {
        disposables.dispose()

        clearables.forEach {
            it.clear()
        }
    }
}
