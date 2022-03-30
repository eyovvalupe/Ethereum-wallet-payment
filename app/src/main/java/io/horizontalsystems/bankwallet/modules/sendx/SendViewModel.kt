package io.horizontalsystems.bankwallet.modules.sendx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.sendevm.AmountInputModule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SendViewModel(private val service: SendBitcoinService) : ViewModel() {

    var uiState by mutableStateOf(
        SendUiState(
            availableBalance = BigDecimal.ZERO,
            fee = BigDecimal.ZERO,
            addressError = null,
            amountError = null,
            canBeSend = false
        )
    )
        private set

    var amountInputMode by mutableStateOf(AmountInputModule.InputMode.Coin)
        private set

    val fiatMaxAllowedDecimals: Int = 2
    val coinMaxAllowedDecimals: Int = 8

    init {
        viewModelScope.launch {
            service.stateFlow
                .collect {
                    uiState = SendUiState(
                        availableBalance = it.availableBalance,
                        fee = it.fee,
                        addressError = it.addressError,
                        amountError = it.amountError,
                        canBeSend = it.canBeSend
                    )
                }
        }
        service.start()
    }

    fun onUpdateAmountInputMode(inputMode: AmountInputModule.InputMode) {
        amountInputMode = inputMode
    }

    fun onEnterAmount(amount: BigDecimal?) {
        service.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        service.setAddress(address)
    }
}

data class SendUiState(
    val availableBalance: BigDecimal,
    val fee: BigDecimal,
    val addressError: Throwable?,
    val amountError: Throwable?,
    val canBeSend: Boolean,
)
