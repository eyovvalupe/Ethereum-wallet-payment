package io.horizontalsystems.bankwallet.modules.send.submodules.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import java.math.BigDecimal

object SendAddressModule {

    interface IView {
        fun setAddress(address: String?)
        fun setAddressError(error: Exception?)
        fun setPasteButtonState(enabled: Boolean)
    }

    interface IViewDelegate {
        fun onViewDidLoad()
        fun onAddressPasteClicked()
        fun onAddressDeleteClicked()
        fun onAddressScanClicked()
    }

    interface IInteractor {
        val addressFromClipboard: String?
        val clipboardHasPrimaryClip: Boolean

        fun parseAddress(address: String): Pair<String, BigDecimal?>
    }

    interface IInteractorDelegate

    interface IAddressModule {
        val currentAddress: String?

        @Throws
        fun validAddress(): String
        fun didScanQrCode(address: String)
    }

    interface IAddressModuleDelegate {
        fun validate(address: String)

        fun onUpdateAddress()
        fun onUpdateAmount(amount: BigDecimal)

        fun scanQrCode()
    }

    open class ValidationError : Exception() {
        class InvalidAddress : ValidationError()
    }


    class Factory(private val coin: Coin,
                  private val sendHandler: SendModule.ISendHandler) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val view = SendAddressView()
            val addressParser = App.addressParserFactory.parser(coin)
            val interactor = SendAddressInteractor(TextHelper, addressParser)
            val presenter = SendAddressPresenter(view, interactor)

            interactor.delegate = presenter
            sendHandler.addressModule = presenter

            return presenter as T
        }
    }

}
