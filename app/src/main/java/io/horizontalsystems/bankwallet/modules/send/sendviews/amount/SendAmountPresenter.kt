package io.horizontalsystems.bankwallet.modules.send.sendviews.amount

import io.horizontalsystems.bankwallet.core.SendStateError
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.send.SendModule
import java.math.BigDecimal
import java.math.RoundingMode


class SendAmountPresenter(
        private val interactor: SendAmountModule.IInteractor,
        private val presenterHelper: SendAmountPresenterHelper,
        private val coinCode: String,
        private val baseCurrency: Currency)
    : SendAmountModule.IViewDelegate, SendAmountModule.IInteractorDelegate {

    var view: SendAmountViewModel? = null

    private var coinAmount: BigDecimal? = null
    private var rate: Rate? = null
    private var error: SendStateError.InsufficientAmount? = null
    private var inputType = SendModule.InputType.COIN

    override val validState: Boolean
        get() {
            return (coinAmount ?: BigDecimal.ZERO) > BigDecimal.ZERO && error == null
        }

    override fun onViewDidLoad() {
        interactor.retrieveRate()
        view?.addTextChangeListener()
        updateAmount()
        updateSwitchButtonState()
    }

    override fun getCoinValue(): CoinValue? {
        return CoinValue(coinCode, coinAmount ?: BigDecimal.ZERO)
    }

    override fun getCurrencyValue(): CurrencyValue? {
        val currencyAmount = rate?.let { coinAmount?.times(it.value) }
        return currencyAmount?.let { CurrencyValue(baseCurrency, it) }
    }

    override fun getInputType(): SendModule.InputType {
        return inputType
    }

    override fun onMaxClick() {
        view?.getAvailableBalance()
    }

    override fun onSwitchClick() {
        view?.removeTextChangeListener()
        val newInputType = when (inputType) {
            SendModule.InputType.CURRENCY -> SendModule.InputType.COIN
            else -> SendModule.InputType.CURRENCY
        }
        inputType = newInputType
        interactor.defaultInputType = newInputType

        updateAmount()
        view?.addTextChangeListener()
        view?.onInputTypeChanged(inputType)

        updateError()
    }

    override fun onAmountChange(amountString: String) {
        updateMaxButtonVisibility(amountString.isEmpty())

        val amount = parseInput(amountString)
        val decimal = presenterHelper.decimal(inputType)
        if(amount.scale() > decimal) {
            onNumberScaleExceeded(amount, decimal)
        } else {
            coinAmount = presenterHelper.getCoinAmount(amount, inputType, rate)
            view?.setHint(presenterHelper.getHint(coinAmount, inputType, rate))
            view?.notifyMainViewModelOnAmountChange(coinAmount)
        }
    }

    override fun didRateRetrieve(rate: Rate?) {
        this.rate = rate
        rate?.let {
            inputType = interactor.defaultInputType
            view?.onInputTypeChanged(inputType)
            updateSwitchButtonState()
        }
        updateAmount()
    }

    override fun onAvailableBalanceRetrieved(availableBalance: BigDecimal) {
        coinAmount = availableBalance
        updateAmount()
    }

    override fun onValidationError(error: SendStateError.InsufficientAmount) {
        this.error = error
        updateError()
    }

    override fun onValidationSuccess() {
        error = null
        view?.setHintErrorBalance(null)
    }

    private fun updateSwitchButtonState() {
        view?.setSwitchButtonEnabled(rate != null)
    }

    private fun updateError() {
        val hintErrorBalance = error?.balance?.let { presenterHelper.getBalanceForHintError(inputType, it, rate) }
        view?.setHintErrorBalance(hintErrorBalance)
    }

    private fun onNumberScaleExceeded(amount: BigDecimal, decimal: Int) {
        val amountNumber = amount.setScale(decimal, RoundingMode.FLOOR)
        val revertedInput = amountNumber.toPlainString()
        view?.revertInput(revertedInput)
    }

    private fun updateMaxButtonVisibility(empty: Boolean) {
        view?.setMaxButtonVisible(empty)
    }

    private fun parseInput(amountString: String): BigDecimal {
        return when {
            amountString != "" -> amountString.toBigDecimalOrNull() ?: BigDecimal.ZERO
            else -> { BigDecimal.ZERO }
        }
    }

    private fun updateAmount() {
        val amount = presenterHelper.getAmount(coinAmount, inputType, rate)
        val hint = presenterHelper.getHint(coinAmount, inputType, rate)
        val prefix = presenterHelper.getAmountPrefix(inputType, rate)

        view?.setAmountPrefix(prefix)
        view?.setAmount(amount)
        view?.setHint(hint)
    }

}
