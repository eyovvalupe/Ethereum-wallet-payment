package io.horizontalsystems.bankwallet.modules.restorewords

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import io.horizontalsystems.bankwallet.core.AccountType
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.SyncMode

object RestoreWordsModule {

    interface IView {
        fun showError(error: Int)
    }

    interface IViewDelegate {
        fun restoreDidClick(words: List<String>)
    }

    interface IInteractor {
        fun validate(words: List<String>)
    }

    interface IInteractorDelegate {
        fun didFailToValidate(exception: Exception)
        fun didValidate(words: List<String>)
    }

    interface IRouter {
        fun navigateToSetSyncMode(words: List<String>)
        fun notifyRestored(accountType: AccountType, syncMode: SyncMode)
    }

    fun startForResult(context: AppCompatActivity, requestCode: Int) {
        val intent = Intent(context, RestoreWordsActivity::class.java)
        context.startActivityForResult(intent, requestCode)
    }

    fun init(view: RestoreWordsViewModel, router: IRouter) {
        val interactor = RestoreWordsInteractor(App.wordsManager)
        val presenter = RestoreWordsPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }
}
