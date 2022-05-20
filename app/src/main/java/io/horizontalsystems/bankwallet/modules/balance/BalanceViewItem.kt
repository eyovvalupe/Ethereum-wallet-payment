package io.horizontalsystems.bankwallet.modules.balance

import androidx.compose.runtime.Immutable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.managers.BigDecimalRounded
import io.horizontalsystems.bankwallet.core.managers.NumberRounding
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.swappable
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.CoinType
import java.math.BigDecimal

@Immutable
data class BalanceViewItem(
    val wallet: Wallet,
    val currencySymbol: String,
    val coinCode: String,
    val coinTitle: String,
    val coinIconUrl: String,
    val coinIconPlaceholder: Int,
    val coinValue: DeemedValue<BigDecimalRounded>,
    val exchangeValue: DeemedValue<BigDecimalRounded?>,
    val diff: BigDecimal?,
    val fiatValue: DeemedValue<BigDecimalRounded?>,
    val coinValueLocked: DeemedValue<BigDecimalRounded>,
    val fiatValueLocked: DeemedValue<BigDecimalRounded?>,
    val expanded: Boolean,
    val sendEnabled: Boolean = false,
    val receiveEnabled: Boolean = false,
    val syncingProgress: SyncingProgress,
    val syncingTextValue: DeemedValue<String?>,
    val syncedUntilTextValue: DeemedValue<String?>,
    val failedIconVisible: Boolean,
    val coinIconVisible: Boolean,
    val badge: String?,
    val swapVisible: Boolean,
    val swapEnabled: Boolean = false,
    val mainNet: Boolean,
    val errorMessage: String?,
    val isWatchAccount: Boolean
)

data class DeemedValue<T>(val value: T, val dimmed: Boolean = false, val visible: Boolean = true)
data class SyncingProgress(val progress: Int?, val dimmed: Boolean = false)

class BalanceViewItemFactory {

    private val numberRounding = NumberRounding()

    private fun coinValue(
        state: AdapterState?,
        balance: BigDecimal,
        visible: Boolean,
        expanded: Boolean,
        coinDecimals: Int
    ): DeemedValue<BigDecimalRounded> {
        val dimmed = state !is AdapterState.Synced
        val rounded = if (expanded) {
            numberRounding.getRoundedCoinFull(balance, coinDecimals)
        } else {
            numberRounding.getRoundedCoinShort(balance, coinDecimals)
        }

        return DeemedValue(rounded, dimmed, visible)
    }

    private fun currencyValue(
        state: AdapterState?,
        balance: BigDecimal,
        coinPrice: CoinPrice?,
        visible: Boolean,
        fullFormat: Boolean
    ): DeemedValue<BigDecimalRounded?> {
        val dimmed = state !is AdapterState.Synced || coinPrice?.expired ?: false
        val value = coinPrice?.value?.let { rate ->
            val balanceFiat = balance * rate
            if (fullFormat) {
                numberRounding.getRoundedCurrencyFull(balanceFiat)
            } else {
                numberRounding.getRoundedCurrencyShort(balanceFiat, 8)
            }
        }

        return DeemedValue(value, dimmed, visible)
    }

    private fun rateValue(coinPrice: CoinPrice?, showSyncing: Boolean): DeemedValue<BigDecimalRounded?> {
        val value = coinPrice?.let {
            numberRounding.getRoundedCurrencyFull(coinPrice.value)
        }

        return DeemedValue(value, dimmed = coinPrice?.expired ?: false, visible = !showSyncing)
    }

    private fun getSyncingProgress(state: AdapterState?, coinType: CoinType): SyncingProgress {
        return when (state) {
            is AdapterState.Syncing -> SyncingProgress(state.progress ?: getDefaultSyncingProgress(coinType), false)
            is AdapterState.SearchingTxs -> SyncingProgress(10, true)
            else -> SyncingProgress(null, false)
        }
    }

    private fun getDefaultSyncingProgress(coinType: CoinType): Int {
        return when (coinType) {
            CoinType.Bitcoin, CoinType.Litecoin, CoinType.BitcoinCash, CoinType.Dash, CoinType.Zcash -> 10
            CoinType.Ethereum, CoinType.BinanceSmartChain, is CoinType.Erc20, is CoinType.Bep2, is CoinType.Bep20,
            CoinType.Polygon, is CoinType.Mrc20 -> 50
            is CoinType.Avalanche,
            is CoinType.Fantom,
            is CoinType.HarmonyShard0,
            is CoinType.HuobiToken,
            is CoinType.Iotex,
            is CoinType.Moonriver,
            is CoinType.OkexChain,
            CoinType.EthereumOptimism,
            CoinType.EthereumArbitrumOne,
            is CoinType.OptimismErc20,
            is CoinType.ArbitrumOneErc20,
            is CoinType.Solana,
            is CoinType.Sora,
            is CoinType.Tomochain,
            is CoinType.Xdai,
            is CoinType.Unsupported -> 0
        }
    }

    private fun getSyncingText(state: AdapterState?, expanded: Boolean): DeemedValue<String?> {
        if (state == null || !expanded) {
            return DeemedValue(null, false, false)
        }

        val text = when (state) {
            is AdapterState.Syncing -> {
                if (state.progress != null) {
                    Translator.getString(R.string.Balance_Syncing_WithProgress, state.progress.toString())
                } else {
                    Translator.getString(R.string.Balance_Syncing)
                }
            }
            is AdapterState.SearchingTxs -> Translator.getString(R.string.Balance_SearchingTransactions)
            else -> null
        }

        return DeemedValue(text, visible = expanded)
    }

    private fun getSyncedUntilText(state: AdapterState?, expanded: Boolean): DeemedValue<String?> {
        if (state == null || !expanded) {
            return DeemedValue(null, false, false)
        }

        val text = when (state) {
            is AdapterState.Syncing -> {
                if (state.lastBlockDate != null) {
                    Translator.getString(R.string.Balance_SyncedUntil, DateHelper.formatDate(state.lastBlockDate, "MMM d, yyyy"))
                } else {
                    null
                }
            }
            is AdapterState.SearchingTxs -> {
                if (state.count > 0) {
                    Translator.getString(R.string.Balance_FoundTx, state.count.toString())
                } else {
                    null
                }
            }
            else -> null
        }

        return DeemedValue(text, visible = expanded)
    }

    private fun lockedCoinValue(
        state: AdapterState?,
        balance: BigDecimal,
        hideBalance: Boolean,
        coinDecimals: Int
    ): DeemedValue<BigDecimalRounded> {
        val visible = !hideBalance && balance > BigDecimal.ZERO
        val deemed = state !is AdapterState.Synced

        val value = numberRounding.getRoundedCoinFull(balance, coinDecimals)

        return DeemedValue(value, deemed, visible)
    }

    fun viewItem(item: BalanceModule.BalanceItem, currency: Currency, expanded: Boolean, hideBalance: Boolean, watchAccount: Boolean): BalanceViewItem {
        val wallet = item.wallet
        val coin = wallet.coin
        val state = item.state
        val latestRate = item.coinPrice

        val showSyncing = expanded && (state is AdapterState.Syncing || state is AdapterState.SearchingTxs)
        val balanceTotalVisibility = !hideBalance && !showSyncing
        val fiatLockedVisibility = !hideBalance && item.balanceData.locked > BigDecimal.ZERO

        return BalanceViewItem(
                wallet = item.wallet,
                currencySymbol = currency.symbol,
                coinCode = coin.code,
                coinTitle = coin.name,
                coinIconUrl = coin.iconUrl,
                coinIconPlaceholder = wallet.coinType.iconPlaceholder,
                coinValue = coinValue(state, item.balanceData.total, balanceTotalVisibility, expanded, wallet.decimal),
                fiatValue = currencyValue(
                    state,
                    item.balanceData.total,
                    latestRate,
                    balanceTotalVisibility,
                    expanded
                ),
                coinValueLocked = lockedCoinValue(
                    state,
                    item.balanceData.locked,
                    hideBalance,
                    wallet.decimal
                ),
                fiatValueLocked = currencyValue(
                    state,
                    item.balanceData.locked,
                    latestRate,
                    fiatLockedVisibility,
                    true
                ),
                exchangeValue = rateValue(latestRate, showSyncing),
                diff = item.coinPrice?.diff,
                expanded = expanded,
                sendEnabled = state is AdapterState.Synced,
                receiveEnabled = state != null,
                syncingProgress = getSyncingProgress(state, wallet.coinType),
                syncingTextValue = getSyncingText(state, expanded),
                syncedUntilTextValue = getSyncedUntilText(state, expanded),
                failedIconVisible = state is AdapterState.NotSynced,
                coinIconVisible = state !is AdapterState.NotSynced,
                badge = wallet.badge,
                swapVisible = item.wallet.coinType.swappable,
                swapEnabled = state is AdapterState.Synced,
                mainNet = item.mainNet,
                errorMessage = (state as? AdapterState.NotSynced)?.error?.message,
                isWatchAccount = watchAccount
        )
    }
}
