package io.horizontalsystems.bankwallet.modules.settings.managekeys

import io.horizontalsystems.bankwallet.core.IPredefinedAccountType
import io.horizontalsystems.bankwallet.entities.*

class ManageKeysPresenter(private val interactor: ManageKeysModule.Interactor, private val router: ManageKeysModule.Router)
    : ManageKeysModule.ViewDelegate, ManageKeysModule.InteractorDelegate {

    var view: ManageKeysModule.View? = null

    private var currentItem: ManageAccountItem? = null

    //  ViewDelegate

    override var items = listOf<ManageAccountItem>()

    override fun viewDidLoad() {
        interactor.loadAccounts()
    }

    override fun onClickNew(accountItem: ManageAccountItem) {
        currentItem = accountItem
        view?.showCreateConfirmation(accountItem.predefinedAccountType.title, accountItem.predefinedAccountType.coinCodes)
    }

    override fun onClickBackup(account: Account) {
        router.startBackupModule(account)
    }

    override fun onClickRestore(accountType: IPredefinedAccountType) {
        when (accountType) {
            is Words12AccountType -> {
                router.startRestoreWords()
            }
            is EosAccountType -> {
                router.startRestoreEos()
            }
        }
    }

    override fun onClickUnlink(accountItem: ManageAccountItem) {
        currentItem = accountItem

        if (accountItem.account?.isBackedUp == true) {
            view?.showUnlinkConfirmation(accountItem)
        } else {
            view?.showBackupConfirmation(accountItem.predefinedAccountType.title)
        }
    }

    override fun onConfirmCreate() {
        try {
            currentItem?.let { interactor.createAccount(it.predefinedAccountType) }
        } catch (e: Exception) {
            view?.showError(e)
        }
    }

    override fun onConfirmBackup() {
        currentItem?.account?.let {
            router.startBackupModule(it)
        }
    }

    override fun onConfirmUnlink(accountId: String) {
        interactor.deleteAccount(accountId)
    }

    override fun onConfirmRestore(accountType: AccountType, syncMode: SyncMode?) {
        interactor.restoreAccount(accountType, syncMode)
    }

    override fun onClear() {
        interactor.clear()
    }

    //  InteractorDelegate

    override fun didLoad(accounts: List<ManageAccountItem>) {
        items = accounts
        view?.show(items)
    }
}
