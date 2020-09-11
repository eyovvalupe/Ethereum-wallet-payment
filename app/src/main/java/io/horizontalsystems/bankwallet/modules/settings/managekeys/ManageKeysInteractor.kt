package io.horizontalsystems.bankwallet.modules.settings.managekeys

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class ManageKeysInteractor(
        private val accountManager: IAccountManager,
        private val walletManager: IWalletManager,
        private val blockchainSettingsManager: IBlockchainSettingsManager,
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager,
        private val priceAlertManager: IPriceAlertManager)
    : ManageKeysModule.Interactor {

    var delegate: ManageKeysModule.InteractorDelegate? = null

    private val disposables = CompositeDisposable()

    override val predefinedAccountTypes: List<PredefinedAccountType>
        get() = predefinedAccountTypeManager.allTypes

    override fun account(predefinedAccountType: PredefinedAccountType): Account? {
        return predefinedAccountTypeManager.account(predefinedAccountType)
    }

    override fun loadAccounts() {
        delegate?.didLoad(mapAccounts())

        accountManager.accountsFlowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    delegate?.didLoad(mapAccounts())
                }
                .let { disposables.add(it) }
    }

    override fun getWallets(): List<Wallet> {
        return walletManager.wallets
    }

    override fun deleteAccount(account: Account) {
        accountManager.delete(account.id)
        priceAlertManager.deleteAlertsByAccountType(account.type)
    }

    override fun clear() {
        disposables.clear()
    }

    private fun mapAccounts(): List<ManageAccountItem> {
        return predefinedAccountTypes.map {

            val account = predefinedAccountTypeManager.account(it)
            ManageAccountItem(it, account , hasDerivationSettings(account))
        }
    }

    private fun hasDerivationSettings(account: Account?): Boolean {

        account?.let {
            return getWallets().find {
                it.account.id == account.id && blockchainSettingsManager.derivationSetting(it.coin.type) != null
            } != null
        }

        return false
    }
}
