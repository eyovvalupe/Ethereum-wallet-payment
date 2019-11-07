package io.horizontalsystems.bankwallet.modules.send.bitcoin

import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SendHodlerModule
import io.horizontalsystems.bankwallet.modules.send.submodules.memo.SendMemoModule
import io.horizontalsystems.hodler.LockTimeInterval
import io.reactivex.Single
import java.math.BigDecimal

class SendBitcoinHandler(private val interactor: SendModule.ISendBitcoinInteractor,
                         private val router: SendModule.IRouter)
    : SendModule.ISendHandler, SendModule.ISendBitcoinInteractorDelegate, SendAmountModule.IAmountModuleDelegate,
      SendAddressModule.IAddressModuleDelegate, SendFeeModule.IFeeModuleDelegate, SendHodlerModule.IHodlerModuleDelegate {

    private fun syncValidation() {
        try {
            amountModule.validAmount()
            addressModule.validAddress()

            delegate.onChange(true)

        } catch (e: Exception) {
            delegate.onChange(false)
        }
    }

    private fun syncAvailableBalance() {
        interactor.fetchAvailableBalance(feeModule.feeRate, addressModule.currentAddress, hodlerModule?.pluginData())
    }

    private fun syncFee() {
        interactor.fetchFee(amountModule.coinAmount.value, feeModule.feeRate, addressModule.currentAddress, hodlerModule?.pluginData())
    }

    private fun syncMinimumAmount() {
        amountModule.setMinimumAmount(interactor.fetchMinimumAmount(addressModule.currentAddress))
        syncValidation()
    }

    private fun syncMaximumAmount() {
        hodlerModule?.let {
            amountModule.setMaximumAmount(interactor.fetchMaximumAmount(it.pluginData()))
            syncValidation()
        }
    }

    // SendModule.ISendHandler

    override lateinit var amountModule: SendAmountModule.IAmountModule
    override lateinit var addressModule: SendAddressModule.IAddressModule
    override lateinit var feeModule: SendFeeModule.IFeeModule
    override lateinit var memoModule: SendMemoModule.IMemoModule
    override var hodlerModule: SendHodlerModule.IHodlerModule? = null

    override lateinit var delegate: SendModule.ISendHandlerDelegate

    override val inputItems: List<SendModule.Input> =
            mutableListOf<SendModule.Input>().apply {
                add(SendModule.Input.Amount)
                add(SendModule.Input.Address)
                if (interactor.isLockTimeEnabled)
                    add(SendModule.Input.Hodler)
                add(SendModule.Input.Fee(true))
                add(SendModule.Input.ProceedButton)
            }

    override fun onModulesDidLoad() {
        syncAvailableBalance()
        syncMinimumAmount()
        syncMaximumAmount()
    }

    override fun onAddressScan(address: String) {
        addressModule.didScanQrCode(address)
    }

    override fun confirmationViewItems(): List<SendModule.SendConfirmationViewItem> {
        return listOf(
                SendModule.SendConfirmationAmountViewItem(amountModule.primaryAmountInfo(),
                                                          amountModule.secondaryAmountInfo(),
                                                          addressModule.validAddress()),
                SendModule.SendConfirmationFeeViewItem(feeModule.primaryAmountInfo, feeModule.secondaryAmountInfo),
                SendModule.SendConfirmationDurationViewItem(feeModule.duration))
    }

    override fun sendSingle(): Single<Unit> {
        return interactor.send(amountModule.validAmount(), addressModule.validAddress(), feeModule.feeRate, hodlerModule?.pluginData())
    }

    // SendModule.ISendBitcoinInteractorDelegate

    override fun didFetchAvailableBalance(availableBalance: BigDecimal) {
        amountModule.setAvailableBalance(availableBalance)
        syncValidation()
    }

    override fun didFetchFee(fee: BigDecimal) {
        feeModule.setFee(fee)
    }

    // SendAmountModule.ModuleDelegate

    override fun onChangeAmount() {
        syncFee()
        syncValidation()
    }

    override fun onChangeInputType(inputType: SendModule.InputType) {
        feeModule.setInputType(inputType)
    }

    // SendAddressModule.ModuleDelegate

    override fun validate(address: String) {
        interactor.validate(address)
    }

    override fun onUpdateAddress() {
        syncAvailableBalance()
        syncFee()
        syncMinimumAmount()
    }

    override fun onUpdateAmount(amount: BigDecimal) {
        amountModule.setAmount(amount)
    }

    override fun scanQrCode() {
        router.scanQrCode()
    }

    // SendFeeModule.IFeeModuleDelegate

    override fun onUpdateFeeRate(feeRate: Long) {
        syncAvailableBalance()
        syncFee()
    }

    override fun onUpdateLockTimeInterval(timeInterval: LockTimeInterval?) {
        syncAvailableBalance()
        syncFee()
        syncMaximumAmount()
    }
}
