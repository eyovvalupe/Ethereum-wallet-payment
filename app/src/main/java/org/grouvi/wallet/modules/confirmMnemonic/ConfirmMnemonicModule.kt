package org.grouvi.wallet.modules.confirmMnemonic

import android.content.Context
import android.content.Intent
import org.grouvi.wallet.lib.WalletDataManager
import java.util.*

object ConfirmMnemonicModule {

    interface IView {
        var presenter: IPresenter

        fun showWordConfirmationForm(confirmationWordPosition: Int)
        fun showWordNotConfirmedError()
    }

    interface IRouter
    interface IPresenter {
        var interactor: IInteractor
        var view: IView

        fun start()
        fun submit(position: Int, word: String)
    }

    interface IInteractor {
        var delegate: IInteractorDelegate
        var walletDataProvider: WalletDataManager
        var random: Random

        fun retrieveConfirmationWord()
        fun validateConfirmationWord(position: Int, word: String)
    }

    interface IInteractorDelegate {
        fun didConfirmationWordRetrieve(wordPosition: Int)
        fun didConfirmationSuccess()
        fun didConfirmationFailure()
    }


    fun start(context: Context) {
        val intent = Intent(context, ConfirmMnemonicActivity::class.java)
        context.startActivity(intent)
    }

    fun initModule(view: IView, router: IRouter) {
        val presenter = ConfirmMnemonicModulePresenter()
        val interactor = ConfirmMnemonicModuleInteractor()

        presenter.view = view
        presenter.interactor = interactor

        interactor.delegate = presenter
        interactor.random = Random()
        interactor.walletDataProvider = WalletDataManager

        view.presenter = presenter
    }
}

class ConfirmMnemonicModuleInteractor : ConfirmMnemonicModule.IInteractor {
    override lateinit var delegate: ConfirmMnemonicModule.IInteractorDelegate
    override lateinit var walletDataProvider: WalletDataManager
    override lateinit var random: Random

    override fun retrieveConfirmationWord() {
        val wordPosition = random.nextInt(walletDataProvider.mnemonicWords.size)
        delegate.didConfirmationWordRetrieve(wordPosition)

    }

    override fun validateConfirmationWord(position: Int, word: String) {
        if (walletDataProvider.mnemonicWords[position] == word) {
            delegate.didConfirmationSuccess()
        } else {
            delegate.didConfirmationFailure()
        }
    }
}

class ConfirmMnemonicModulePresenter : ConfirmMnemonicModule.IPresenter, ConfirmMnemonicModule.IInteractorDelegate {
    override lateinit var interactor: ConfirmMnemonicModule.IInteractor
    override lateinit var view: ConfirmMnemonicModule.IView

    // IPresenter
    override fun start() {
        interactor.retrieveConfirmationWord()
    }

    override fun submit(position: Int, word: String) {
        interactor.validateConfirmationWord(position, word)

    }

    // IInteractorDelegate
    override fun didConfirmationWordRetrieve(wordPosition: Int) {
        view.showWordConfirmationForm(wordPosition)
    }

    override fun didConfirmationSuccess() {
//        todo
    }

    override fun didConfirmationFailure() {
        view.showWordNotConfirmedError()
    }
}