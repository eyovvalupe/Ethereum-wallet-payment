package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.IBuildConfigProvider
import io.horizontalsystems.core.ILanguageConfigProvider
import io.horizontalsystems.core.entities.Currency

class AppConfigProvider : IAppConfigProvider, ILanguageConfigProvider, IBuildConfigProvider {

    override val companyWebPageLink: String = "https://horizontalsystems.io"
    override val appWebPageLink: String = "https://unstoppable.money"
    override val appGithubLink: String = "https://github.com/horizontalsystems/unstoppable-wallet-android"
    override val reportEmail = "support.unstoppable@protonmail.com"
    override val btcCoreRpcUrl: String = "https://btc.horizontalsystems.xyz/rpc"
    override val notificationUrl: String = "https://pns-dev.horizontalsystems.xyz/api/v1/pns/"
    override val releaseNotesUrl: String = "https://api.github.com/repos/horizontalsystems/unstoppable-wallet-android/releases/tags/"

    override val cryptoCompareApiKey by lazy {
        Translator.getString(R.string.cryptoCompareApiKey)
    }
    override val infuraProjectId by lazy {
        Translator.getString(R.string.infuraProjectId)
    }
    override val infuraProjectSecret by lazy {
        Translator.getString(R.string.infuraSecretKey)
    }
    override val etherscanApiKey by lazy {
        Translator.getString(R.string.etherscanKey)
    }
    override val bscscanApiKey by lazy {
        Translator.getString(R.string.bscscanKey)
    }
    override val guidesUrl by lazy {
        Translator.getString(R.string.guidesUrl)
    }
    override val faqUrl by lazy {
        Translator.getString(R.string.faqUrl)
    }

    override val fiatDecimal: Int = 2
    override val maxDecimal: Int = 8
    override val feeRateAdjustForCurrencies: List<String> = listOf("USD", "EUR")

    override val currencies: List<Currency> = listOf(
            Currency(code = "USD", symbol = "\u0024", decimal = 2),
            Currency(code = "EUR", symbol = "\u20AC", decimal = 2),
            Currency(code = "GBP", symbol = "\u00A3", decimal = 2),
            Currency(code = "JPY", symbol = "\u00A5", decimal = 2)
    )
    override val featuredCoinTypes: List<CoinType> = listOf(
            CoinType.Bitcoin,
            CoinType.BitcoinCash,
            CoinType.Ethereum,
            CoinType.Zcash,
            CoinType.BinanceSmartChain
    )

    //  ILanguageConfigProvider

    override val localizations: List<String>
        get() {
            val coinsString = "de,en,es,fa,fr,ko,ru,tr,zh"
            return coinsString.split(",")
        }

    //  IBuildConfigProvider

    override val testMode: Boolean = BuildConfig.testMode

}
