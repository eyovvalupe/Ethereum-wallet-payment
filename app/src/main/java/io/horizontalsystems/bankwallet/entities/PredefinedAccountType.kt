package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import kotlinx.android.parcel.Parcelize

sealed class PredefinedAccountType : Parcelable {
    @Parcelize
    object Standard : PredefinedAccountType()

    @Parcelize
    object Eos : PredefinedAccountType()

    @Parcelize
    object Binance : PredefinedAccountType()

    @Parcelize
    object Zcash : PredefinedAccountType()

    val title: Int
        get() = when (this) {
            Standard -> R.string.AccountType_Unstoppable
            Eos -> R.string.AccountType_Eos
            Binance -> R.string.AccountType_Binance
            Zcash -> R.string.AccountType_Zcash
        }

    val coinCodes: Int
        get() = when (this) {
            Standard -> R.string.AccountType_Unstoppable_Text
            Eos -> R.string.AccountType_Eos_Text
            Binance -> R.string.AccountType_Binance_Text
            Zcash -> R.string.AccountType_Zcash_Text
        }

    fun supports(accountType: AccountType) = when (this) {
        Standard -> {
            accountType is AccountType.Mnemonic && accountType.words.size == 12
        }
        Eos -> {
            accountType is AccountType.Eos
        }
        Binance -> {
            accountType is AccountType.Mnemonic && accountType.words.size == 24
        }
        Zcash -> {
            accountType is AccountType.Zcash && accountType.words.size == 24
        }
    }

    fun isCreationSupported(): Boolean = when (this) {
        Eos -> false
        else -> true
    }

    override fun toString(): String {
        return when (this) {
            Standard -> STANDARD
            Eos -> EOS
            Binance -> BINANCE
            Zcash -> ZCASH
        }
    }

    companion object {
        const val STANDARD = "standard"
        const val EOS = "eos"
        const val BINANCE = "binance"
        const val ZCASH = "zcash"
    }
}
