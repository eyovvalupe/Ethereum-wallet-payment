package io.horizontalsystems.bankwallet.modules.createwallet

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*

class CreateWalletInteractor(
        private val coinManager: ICoinManager,
        private val accountCreator: IAccountCreator,
        private val accountManager: IAccountManager,
        private val walletManager: IWalletManager,
        private val blockchainSettingsManager: IBlockchainSettingsManager
) : CreateWalletModule.IInteractor {

    override val coins: List<Coin>
        get() = coinManager.coins

    override val featuredCoins: List<Coin>
        get() = coinManager.featuredCoins

    override fun createAccounts(accounts: List<Account>) {
        accounts.forEach {
            accountManager.save(it)
        }
    }

    override fun saveWallets(wallets: List<Wallet>) {
        walletManager.save(wallets)
    }

    @Throws
    override fun account(predefinedAccountType: PredefinedAccountType): Account {
        return accountCreator.newAccount(predefinedAccountType)
    }

    override fun initializeWithDefaultSettings(coinType: CoinType) {
        blockchainSettingsManager.initializeSettingsWithDefault(coinType)
    }
}
