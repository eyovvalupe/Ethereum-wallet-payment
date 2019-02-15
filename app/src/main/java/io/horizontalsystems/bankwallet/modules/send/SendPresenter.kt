package io.horizontalsystems.bankwallet.modules.send

import java.math.BigDecimal

class SendPresenter(
        private val interactor: SendModule.IInteractor,
        private val factory: StateViewItemFactory,
        private val userInput: SendModule.UserInput
) : SendModule.IViewDelegate, SendModule.IInteractorDelegate {

    var view: SendModule.IView? = null

    //
    // IViewDelegate
    //
    override fun onViewDidLoad() {
        val state = interactor.stateForUserInput(userInput)
        val viewItem = factory.viewItemForState(state)

        view?.setCoin(interactor.coin)
        view?.setDecimal(viewItem.decimal)
        view?.setAmountInfo(viewItem.amountInfo)
        view?.setSwitchButtonEnabled(viewItem.switchButtonEnabled)
        view?.setHintInfo(viewItem.hintInfo)
        view?.setAddressInfo(viewItem.addressInfo)
        view?.setFeeInfo(viewItem.feeInfo)
        view?.setSendButtonEnabled(viewItem.sendButtonEnabled)
        updatePasteButtonState()

        interactor.retrieveRate()
    }

    override fun onAmountChanged(amount: BigDecimal) {
        userInput.amount = amount

        val state = interactor.stateForUserInput(userInput)
        val viewItem = factory.viewItemForState(state)

        view?.setHintInfo(viewItem.hintInfo)
        view?.setFeeInfo(viewItem.feeInfo)
        view?.setSendButtonEnabled(viewItem.sendButtonEnabled)
    }

    override fun onMaxClicked() {
        val totalBalanceMinusFee = interactor.getTotalBalanceMinusFee(userInput.inputType, userInput.address)
        userInput.amount = totalBalanceMinusFee

        val state = interactor.stateForUserInput(userInput)
        val viewItem = factory.viewItemForState(state, true)

        view?.setAmountInfo(viewItem.amountInfo)
    }

    override fun onSwitchClicked() {
        val convertedAmount = interactor.convertedAmountForInputType(userInput.inputType, userInput.amount)
                ?: return

        val newInputType = when (userInput.inputType) {
            SendModule.InputType.CURRENCY -> SendModule.InputType.COIN
            else -> SendModule.InputType.CURRENCY
        }

        userInput.amount = convertedAmount
        userInput.inputType = newInputType

        val state = interactor.stateForUserInput(userInput)
        val viewItem = factory.viewItemForState(state)

        view?.setDecimal(viewItem.decimal)
        view?.setAmountInfo(viewItem.amountInfo)
        view?.setHintInfo(viewItem.hintInfo)
        view?.setFeeInfo(viewItem.feeInfo)

        interactor.defaultInputType = newInputType
    }

    override fun onPasteClicked() {
        interactor.addressFromClipboard?.let {
            onAddressEnter(it)
        }
    }

    override fun onScanAddress(address: String) {
        onAddressEnter(address)
    }

    override fun onDeleteClicked() {
        onAddressChange(null)
        updatePasteButtonState()
    }

    override fun onSendClicked() {
        val state = interactor.stateForUserInput(userInput)
        val viewItem = factory.confirmationViewItemForState(state) ?: return

        view?.showConfirmation(viewItem)
    }

    override fun onConfirmClicked() {
        interactor.send(userInput)
    }

    override fun onClear() {
        interactor.clear()
    }

    //
    // IInteractorDelegate
    //
    override fun didRateRetrieve() {
        if (interactor.defaultInputType == SendModule.InputType.CURRENCY && userInput.amount == BigDecimal.ZERO) {
            userInput.inputType = interactor.defaultInputType
        }

        val state = interactor.stateForUserInput(userInput)
        val viewItem = factory.viewItemForState(state)

        view?.setDecimal(viewItem.decimal)
        view?.setAmountInfo(viewItem.amountInfo)
        view?.setSwitchButtonEnabled(viewItem.switchButtonEnabled)
        view?.setHintInfo(viewItem.hintInfo)
        view?.setFeeInfo(viewItem.feeInfo)
    }

    override fun didSend() {
        view?.dismissWithSuccess()
    }

    override fun didFailToSend(error: Throwable) {
        view?.showError(error)
    }

    //
    // Private
    //
    private fun updatePasteButtonState() {
        view?.setPasteButtonState(interactor.clipboardHasPrimaryClip)
    }

    private fun onAddressEnter(address: String) {
        val paymentAddress = interactor.parsePaymentAddress(address)
        paymentAddress.amount?.let {
            userInput.amount = it
        }

        onAddressChange(paymentAddress.address)
    }

    private fun onAddressChange(address: String?) {
        userInput.address = address

        val state = interactor.stateForUserInput(userInput)
        val viewItem = factory.viewItemForState(state)

        view?.setAddressInfo(viewItem.addressInfo)
        view?.setAmountInfo(viewItem.amountInfo)
        view?.setFeeInfo(viewItem.feeInfo)
        view?.setSendButtonEnabled(viewItem.sendButtonEnabled)
    }

}
