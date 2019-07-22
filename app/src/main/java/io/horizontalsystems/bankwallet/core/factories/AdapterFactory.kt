package io.horizontalsystems.bankwallet.core.factories

import android.content.Context
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.utils.AddressParser
import io.horizontalsystems.bankwallet.entities.CoinType

class AdapterFactory(
        private val context: Context,
        private val appConfigProvider: IAppConfigProvider,
        private val ethereumKitManager: IEthereumKitManager,
        private val eosKitManager: IEosKitManager,
        private val feeRateProvider: IFeeRateProvider) {

    fun adapterForCoin(wallet: Wallet): IAdapter? = when (wallet.coin.type) {
        is CoinType.Bitcoin -> BitcoinAdapter(wallet, appConfigProvider.testMode, feeRateProvider)
        is CoinType.BitcoinCash -> BitcoinCashAdapter(wallet, appConfigProvider.testMode, feeRateProvider)
        is CoinType.Dash -> DashAdapter(wallet, appConfigProvider.testMode, feeRateProvider)
        is CoinType.Ethereum -> {
            val addressParser = AddressParser("ethereum", true)
            EthereumAdapter(wallet, ethereumKitManager.ethereumKit(wallet), addressParser, feeRateProvider)
        }
        is CoinType.Erc20 -> {
            val addressParser = AddressParser("ethereum", true)
            Erc20Adapter(context, wallet, ethereumKitManager.ethereumKit(wallet), wallet.coin.type.decimal, wallet.coin.type.fee, wallet.coin.type.address, addressParser, feeRateProvider)
        }
        is CoinType.Eos -> {
            EosAdapter(wallet, wallet.coin.type, eosKitManager.eosKit(wallet))
        }
    }

    fun unlinkAdapter(adapter: IAdapter) {
        when (adapter) {
            is EthereumBaseAdapter -> {
                ethereumKitManager.unlink()
            }
            is EosBaseAdapter -> {
                eosKitManager.unlink()
            }
        }
    }
}
