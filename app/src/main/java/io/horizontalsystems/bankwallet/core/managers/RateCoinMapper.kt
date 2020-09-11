package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IRateCoinMapper

class RateCoinMapper : IRateCoinMapper {
    private val disabledCoins = setOf("SAI", "PGL", "PPT", "EOSDT", "WBTC", "WETH", "SWAP")
    private val convertedCoins = mapOf("HOT" to "HOLO")

    override fun convert(coinCode: String): String? {
        return if (disabledCoins.contains(coinCode)) {
            null
        } else {
            convertedCoins.getOrDefault(coinCode, coinCode)
        }
    }

    override fun unconvert(coinCode: String): String {
        convertedCoins.forEach { (from, to) ->
            if (to == coinCode) {
                return from
            }
        }
        return coinCode
    }

}
