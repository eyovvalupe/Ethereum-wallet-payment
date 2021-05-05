package io.horizontalsystems.bankwallet.modules.send.submodules.amount

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.ui.extensions.AmountInputView
import io.horizontalsystems.core.SingleLiveEvent

class SendAmountView : SendAmountModule.IView {

    val amount = MutableLiveData<String>()
    val availableBalance = MutableLiveData<String>()
    val hint = MutableLiveData<String>()
    val maxButtonVisibleValue = MutableLiveData<Boolean>()
    val revertAmount = SingleLiveEvent<String>()
    val validationError = MutableLiveData<SendAmountModule.ValidationError?>()
    val setLoading = MutableLiveData<Boolean>()
    val inputParamsLiveData = MutableLiveData<AmountInputView.InputParams>()


    override fun setAmount(amount: String) {
        this.amount.postValue(amount)
    }

    override fun setAvailableBalance(availableBalance: String) {
        this.availableBalance.postValue(availableBalance)
    }

    override fun setHint(hint: String) {
        this.hint.postValue(hint)
    }

    override fun setMaxButtonVisible(visible: Boolean) {
        maxButtonVisibleValue.postValue(visible)
    }

    override fun revertAmount(amount: String) {
        revertAmount.value = amount
    }

    override fun setValidationError(error: SendAmountModule.ValidationError?) {
        this.validationError.postValue(error)
    }

    override fun setLoading(loading: Boolean) {
        setLoading.postValue(loading)
    }

    override fun setInputFields(inputParams: AmountInputView.InputParams) {
        inputParamsLiveData.postValue(inputParams)
    }
}
