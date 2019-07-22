package io.horizontalsystems.bankwallet.modules.backup

import android.content.Context
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account

object BackupModule {

    interface View

    interface ViewDelegate {
        fun onClickCancel()
        fun onClickBackup()
        fun didBackUp(accountId: String)
        fun didUnlock()
        fun didCancelUnlock()
    }

    interface Interactor {
        val isPinSet: Boolean

        fun setBackedUp(accountId: String)
    }

    interface InteractorDelegate

    interface Router {
        fun startUnlockPinModule()
        fun startBackupModule(account: Account)
        fun close()
    }

    //  helpers

    fun start(context: Context, account: Account) {
        BackupActivity.start(context, account)
    }

    fun init(view: BackupViewModel, router: Router, account: Account) {
        val interactor = BackupInteractor(App.backupManager, App.pinManager)
        val presenter = BackupPresenter(interactor, router, account)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
