package io.horizontalsystems.bankwallet.modules.swap.view

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.swap.model.AmountType
import io.horizontalsystems.bankwallet.modules.swap.model.PriceImpact
import io.horizontalsystems.bankwallet.modules.swap.provider.StringProvider
import java.math.BigDecimal
import java.math.RoundingMode

class SwapItemFormatter(
        private val stringProvider: StringProvider,
        private val numberFormatter: IAppNumberFormatter
) {

    fun minMaxTitle(amountType: AmountType): String {
        return if (amountType == AmountType.ExactSending)
            stringProvider.string(R.string.Swap_MinimumReceived)
        else
            stringProvider.string(R.string.Swap_MaximumSold)
    }

    fun minMaxValue(amount: BigDecimal, coinSending: Coin, coinReceiving: Coin, amountType: AmountType): String {
        return when (amountType) {
            AmountType.ExactSending -> {
                 coinAmount(amount, coinReceiving)
            }
            AmountType.ExactReceiving -> {
                coinAmount(amount, coinSending)
            }
        }
    }

    fun executionPrice(price: BigDecimal, coinSending: Coin, coinReceiving: Coin): String {
        val inversePrice = if (price == BigDecimal.ZERO)
            BigDecimal.ZERO
        else
            BigDecimal.ONE.divide(price, price.scale(), RoundingMode.HALF_UP)

        return "${coinReceiving.code} = ${coinAmount(inversePrice, coinSending)} "
    }

    fun priceImpact(priceImpact: PriceImpact): String {
        return stringProvider.string(R.string.Swap_Percent, priceImpact.value.toPlainString())
    }

    fun fee(fee: Pair<CoinValue, CurrencyValue?>): String {
        val coinAmount = fee.first.let { coinAmount(it.value, it.coin) }
        val fiatAmount = fee.second?.let {
            numberFormatter.formatFiat(it.value, it.currency.symbol, 2, 2)
        }
        return "$coinAmount${if (fiatAmount != null) " | $fiatAmount" else ""}"
    }

    fun coinAmount(amount: BigDecimal, coin: Coin): String {
        val maxFraction = if (coin.decimal < 8) coin.decimal else 8
        return numberFormatter.formatCoin(amount, coin.code, 0, maxFraction)
    }
}
