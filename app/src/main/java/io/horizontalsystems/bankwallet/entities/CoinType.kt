package io.horizontalsystems.bankwallet.entities

import java.io.Serializable
import java.math.BigDecimal

sealed class CoinType : Serializable {
    object BitcoinCash : CoinType()
    object Bitcoin : CoinType()
    object Dash : CoinType()
    object Ethereum : CoinType()

    class Erc20(val address: String, val fee: BigDecimal = BigDecimal.ZERO, val gasLimit: Long = 100_000, val minimumRequiredBalance: BigDecimal = BigDecimal.ZERO, val minimumSendAmount: BigDecimal = BigDecimal.ZERO) : CoinType()
    class Eos(val token: String, val symbol: String) : CoinType()
    class Binance(val symbol: String) : CoinType()

    fun canSupport(accountType: AccountType): Boolean {
        when (this) {
            is Eos -> {
                return accountType is AccountType.Eos
            }
            is Bitcoin, BitcoinCash, Dash, Ethereum, is Erc20 -> {
                if (accountType is AccountType.Mnemonic) {
                    return accountType.words.size == 12 && accountType.salt == null
                }
            }
            is Binance -> {
                if (accountType is AccountType.Mnemonic) {
                    return accountType.words.size == 24 && accountType.salt == null
                }
            }
        }

        return false
    }

    fun typeLabel(): String? {
        return when (this) {
            is Erc20 -> "ERC20"
            is Eos -> if (symbol != "EOS") "EOS" else null
            is Binance -> if (symbol != "BNB") "BEP2" else null
            else -> null
        }
    }

    val predefinedAccountType: PredefinedAccountType
        get() = when (this) {
            is Bitcoin,
            is Erc20,
            is BitcoinCash,
            is Dash,
            is Ethereum -> PredefinedAccountType.Standard
            is Binance -> PredefinedAccountType.Binance
            is Eos -> PredefinedAccountType.Eos
        }

    val settings: List<CoinSetting>
         get() = when (this) {
            is Bitcoin ->  listOf(CoinSetting.Derivation, CoinSetting.SyncMode)
            is BitcoinCash -> listOf(CoinSetting.SyncMode)
            is Dash ->  listOf(CoinSetting.SyncMode)
            else -> listOf()
        }

}

enum class CoinSetting {
    Derivation,
    SyncMode
}

typealias CoinSettings = MutableMap<CoinSetting, String>
