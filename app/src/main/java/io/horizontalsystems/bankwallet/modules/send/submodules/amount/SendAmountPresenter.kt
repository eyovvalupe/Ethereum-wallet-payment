package io.horizontalsystems.bankwallet.modules.send.submodules.amount

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo.CoinValueInfo
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo.CurrencyValueInfo
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule.ValidationError.InsufficientBalance
import java.math.BigDecimal
import java.math.RoundingMode


class SendAmountPresenter(
        val view: SendAmountModule.IView,
        private val interactor: SendAmountModule.IInteractor,
        private val presenterHelper: SendAmountPresenterHelper,
        private val coin: Coin,
        private val baseCurrency: Currency)
    : ViewModel(), SendAmountModule.IViewDelegate, SendAmountModule.IAmountModule {

    var moduleDelegate: SendAmountModule.IAmountModuleDelegate? = null

    private var amount: BigDecimal? = null
    private var availableBalance: BigDecimal? = null
    private var xRate: BigDecimal? = null

    override var inputType = SendModule.InputType.COIN
        private set

    override val coinAmount: CoinValue
        get() = CoinValue(coin, amount ?: BigDecimal.ZERO)

    override val fiatAmount: CurrencyValue?
        get() {
            val currencyAmount = xRate?.let { amount?.times(it) }
            return currencyAmount?.let { CurrencyValue(baseCurrency, it) }
        }

    override val currentAmount: BigDecimal
        get() = amount ?: BigDecimal.ZERO


    @Throws
    override fun primaryAmountInfo(): SendModule.AmountInfo {
        return when (inputType) {
            SendModule.InputType.COIN -> CoinValueInfo(CoinValue(coin, validAmount()))
            SendModule.InputType.CURRENCY -> {
                this.xRate?.let { xRate ->
                    CurrencyValueInfo(CurrencyValue(baseCurrency, validAmount() * xRate))
                } ?: throw Exception("Invalid state")
            }
        }
    }

    override fun secondaryAmountInfo(): SendModule.AmountInfo? {
        return when (inputType.reversed()) {
            SendModule.InputType.COIN -> CoinValueInfo(CoinValue(coin, validAmount()))
            SendModule.InputType.CURRENCY -> {
                this.xRate?.let { xRate ->
                    CurrencyValueInfo(CurrencyValue(baseCurrency, validAmount() * xRate))
                }
            }
        }
    }

    @Throws
    override fun validAmount(): BigDecimal {
        val amount = this.amount ?: BigDecimal.ZERO

        if (amount <= BigDecimal.ZERO) {
            throw SendAmountModule.ValidationError.EmptyValue("amount")
        }

        validate()

        return amount
    }

    override fun setAmount(amount: BigDecimal) {
        this.amount = amount

        syncAmount()
        syncHint()
        syncMaxButton()
        syncError()

        moduleDelegate?.onChangeAmount()
    }

    override fun setAvailableBalance(availableBalance: BigDecimal) {
        this.availableBalance = availableBalance

        syncError()
    }

    // SendModule.IViewDelegate

    override fun onViewDidLoad() {
        xRate = interactor.getRate()

        inputType = when {
            xRate == null -> SendModule.InputType.COIN
            else -> interactor.defaultInputType
        }

        moduleDelegate?.onChangeInputType(inputType)

        syncAmountType()
        syncSwitchButton()
        syncAmount()
        syncHint()

        view.addTextChangeListener()
    }

    override fun onSwitchClick() {
        view.removeTextChangeListener()

        inputType = when (inputType) {
            SendModule.InputType.CURRENCY -> SendModule.InputType.COIN
            else -> SendModule.InputType.CURRENCY
        }
        interactor.defaultInputType = inputType
        moduleDelegate?.onChangeInputType(inputType)

        syncAmountType()
        syncAmount()
        syncHint()
        syncError()

        view.addTextChangeListener()
    }

    override fun onAmountChange(amountString: String) {
        val amount = amountString.toBigDecimalOrNull()
        val decimal = presenterHelper.decimal(inputType)

        if (amount != null && amount.scale() > decimal) {
            val amountNumber = amount.setScale(decimal, RoundingMode.FLOOR)
            val revertedInput = amountNumber.toPlainString()
            view.revertAmount(revertedInput)
        } else {
            this.amount = presenterHelper.getCoinAmount(amount, inputType, xRate)

            syncHint()
            syncMaxButton()
            syncError()

            moduleDelegate?.onChangeAmount()
        }
    }

    override fun onMaxClick() {
        amount = availableBalance

        view.removeTextChangeListener()

        syncAmount()
        syncHint()
        syncMaxButton()
        syncError()

        moduleDelegate?.onChangeAmount()
        view.addTextChangeListener()
    }

    private fun syncAmount() {
        val amount = presenterHelper.getAmount(amount, inputType, xRate)
        view.setAmount(amount)
    }

    private fun syncAmountType() {
        val prefix = presenterHelper.getAmountPrefix(inputType, xRate)
        view.setAmountType(prefix)
    }

    private fun syncHint() {
        val hint = presenterHelper.getHint(this.amount, inputType, xRate)
        view.setHint(hint)
    }

    private fun syncMaxButton() {
        val visible = amount?.let { it == BigDecimal.ZERO } ?: true
        view.setMaxButtonVisible(visible)
    }

    private fun syncSwitchButton() {
        view.setSwitchButtonEnabled(xRate != null)
    }

    private fun validate() {
        val amount = this.amount ?: return
        val availableBalance = this.availableBalance ?: return

        if (availableBalance < amount) {
            val amountInfo = when (inputType) {
                SendModule.InputType.COIN -> {
                    CoinValueInfo(CoinValue(coin, availableBalance))
                }
                SendModule.InputType.CURRENCY -> {
                    xRate?.let { rate ->
                        val value = availableBalance.times(rate)
                        CurrencyValueInfo(CurrencyValue(baseCurrency, value))
                    }
                }
            }
            throw InsufficientBalance(amountInfo)
        }
    }

    private fun syncError() {
        try {
            validate()
            view.setHintErrorBalance(null)
        } catch (insufficientBalance: InsufficientBalance) {
            view.setHintErrorBalance(insufficientBalance.availableBalance?.getFormatted())
        }
    }

}
