package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IEnabledWalletStorage
import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.entities.*

class WalletStorage(
        private val coinManager: ICoinManager,
        private val storage: IEnabledWalletStorage)
    : IWalletStorage {

    override fun wallets(accounts: List<Account>): List<Wallet> {
        val coins = coinManager.coins

        val enabledWallets = storage.enabledWallets
        return enabledWallets.map { enabledWallet ->
            val coin = coins.find { it.id == enabledWallet.coinId } ?: return@map null
            val account = accounts.find { it.id == enabledWallet.accountId } ?: return@map null

            val settings = CoinSettings(enabledWallet.coinSettingsId)
            val configuredCoin = ConfiguredCoin(coin, settings)

            Wallet(configuredCoin, account)
        }.mapNotNull { it }
    }

    override fun wallets(account: Account): List<Wallet> {
        val coins = coinManager.coins

        val enabledWallets = storage.enabledWallets(account.id)
        return enabledWallets.mapNotNull { enabledWallet ->
            val coin = coins.find { it.id == enabledWallet.coinId } ?: return@mapNotNull null

            val settings = CoinSettings(enabledWallet.coinSettingsId)
            val configuredCoin = ConfiguredCoin(coin, settings)

            Wallet(configuredCoin, account)
        }
    }

    override fun save(wallets: List<Wallet>) {
        val enabledWallets = mutableListOf<EnabledWallet>()

        wallets.forEachIndexed { index, wallet ->

            enabledWallets.add(
                    EnabledWallet(
                            wallet.coin.id,
                            wallet.configuredCoin.settings.id,
                            wallet.account.id,
                            index
                    )
            )
        }

        storage.save(enabledWallets)
    }

    override fun delete(wallets: List<Wallet>) {
        val enabledWallets = wallets.map {
            EnabledWallet(it.coin.id, it.configuredCoin.settings.id, it.account.id)
        }
        storage.delete(enabledWallets)
    }
}
