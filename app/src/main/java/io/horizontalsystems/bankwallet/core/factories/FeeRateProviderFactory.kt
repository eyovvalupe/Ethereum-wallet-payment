package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.bankwallet.core.providers.*
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType

object FeeRateProviderFactory {
    private val feeRateProvider = App.feeRateProvider

    fun provider(coin: Coin): IFeeRateProvider? {
        return when (coin.type) {
            is CoinType.Bitcoin -> BitcoinFeeRateProvider(feeRateProvider)
            is CoinType.Litecoin -> LitecoinFeeRateProvider(feeRateProvider)
            is CoinType.BitcoinCash -> BitcoinCashFeeRateProvider(feeRateProvider)
            is CoinType.Dash -> DashFeeRateProvider(feeRateProvider)
            is CoinType.Ethereum, is CoinType.Erc20 -> EthereumFeeRateProvider(feeRateProvider)
            else -> null
        }
    }

}
