package io.horizontalsystems.bankwallet.modules.send

class SendPresenter(
        private val interactor: SendModule.IInteractor,
        private val router: SendModule.IRouter,
        private val factory: StateViewItemFactory,
        private val userInput: SendModule.UserInput
) : SendModule.IViewDelegate, SendModule.IInteractorDelegate {

    var view: SendModule.IView? = null

    override fun onViewDidLoad() {
        interactor.retrieveRate()
    }

    override fun onAmountChanged(amount: Double) {
        userInput.amount = amount

        val state = interactor.stateForUserInput(userInput)
        val viewItem = factory.viewItemForState(state)

        view?.setHintInfo(viewItem.hintInfo)
        view?.setPrimaryFeeInfo(viewItem.primaryFeeInfo)
        view?.setSecondaryFeeInfo(viewItem.secondaryFeeInfo)
        view?.setSendButtonEnabled(viewItem.sendButtonEnabled)
    }

    override fun onSwitchClicked() {
        val convertedAmount = interactor.convertedAmountForInputType(userInput.inputType, userInput.amount)
                ?: return

        userInput.amount = convertedAmount
        userInput.inputType = when (userInput.inputType) {
            SendModule.InputType.CURRENCY -> SendModule.InputType.COIN
            else -> SendModule.InputType.CURRENCY
        }

        val state = interactor.stateForUserInput(userInput)
        val viewItem = factory.viewItemForState(state)

        view?.setAmountInfo(viewItem.amountInfo)
        view?.setHintInfo(viewItem.hintInfo)
        view?.setPrimaryFeeInfo(viewItem.primaryFeeInfo)
        view?.setSecondaryFeeInfo(viewItem.secondaryFeeInfo)
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
    }

    override fun onSendClicked() {
        val state = interactor.stateForUserInput(userInput)
        val viewItem = factory.confirmationViewItemForState(state) ?: return

        view?.showConfirmation(viewItem)
    }

    override fun onConfirmClicked() {
        interactor.send(userInput)
    }

    override fun didRateRetrieve() {
        val state = interactor.stateForUserInput(userInput)
        val viewItem = factory.viewItemForState(state)

        view?.setCoin(interactor.coin)
        view?.setAmountInfo(viewItem.amountInfo)
        view?.setSwitchButtonEnabled(viewItem.switchButtonEnabled)
        view?.setHintInfo(viewItem.hintInfo)
        view?.setAddressInfo(viewItem.addressInfo)
        view?.setPrimaryFeeInfo(viewItem.primaryFeeInfo)
        view?.setSecondaryFeeInfo(viewItem.secondaryFeeInfo)
        view?.setSendButtonEnabled(viewItem.sendButtonEnabled)
    }

    override fun didSend() {
        view?.dismissWithSuccess()
    }

    override fun didFailToSend(error: Throwable) {
        view?.showError(error)
    }

    private fun onAddressEnter(address: String) {
        val paymentAddress = interactor.parsePaymentAddress(address)
        paymentAddress.amount?.let{
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
        view?.setPrimaryFeeInfo(viewItem.primaryFeeInfo)
        view?.setSecondaryFeeInfo(viewItem.secondaryFeeInfo)
        view?.setSendButtonEnabled(viewItem.sendButtonEnabled)
    }

}
