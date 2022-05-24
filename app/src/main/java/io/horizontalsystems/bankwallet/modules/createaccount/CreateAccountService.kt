package io.horizontalsystems.bankwallet.modules.createaccount

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.PassphraseValidator
import io.horizontalsystems.bankwallet.core.managers.WalletActivator
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.subjects.BehaviorSubject

class CreateAccountService(
    private val accountFactory: IAccountFactory,
    private val wordsManager: WordsManager,
    private val accountManager: IAccountManager,
    private val walletManager: IWalletManager,
    private val walletActivator: WalletActivator,
    private val passphraseValidator: PassphraseValidator,
    private val marketKit: MarketKit
) : Clearable {

    val allKinds: Array<CreateAccountModule.Kind> = CreateAccountModule.Kind.values()

    var kind: CreateAccountModule.Kind = CreateAccountModule.Kind.Mnemonic12
        set(value) {
            field = value
            kindObservable.onNext(value)
        }
    val kindObservable = BehaviorSubject.createDefault(kind)

    var passphraseEnabled: Boolean = false
        set(value) {
            field = value
            passphraseEnabledObservable.onNext(value)
        }
    val passphraseEnabledObservable = BehaviorSubject.createDefault(passphraseEnabled)

    var passphrase = ""
    var passphraseConfirmation = ""

    override fun clear() = Unit

    fun createAccount() {
        if (passphraseEnabled) {
            if (passphrase.isBlank()) throw CreateError.EmptyPassphrase
            if (passphrase != passphraseConfirmation) throw CreateError.InvalidConfirmation
        }

        val accountType = resolveAccountType()
        val account = accountFactory.account(accountFactory.getNextAccountName(), accountType, AccountOrigin.Created, false)

        accountManager.save(account)
        activateDefaultWallets(account)
    }

    private fun activateDefaultWallets(account: Account) {
        walletActivator.activateWallets(account, listOf(CoinType.Bitcoin, CoinType.Ethereum, CoinType.BinanceSmartChain, CoinType.Polygon, CoinType.Zcash))
    }

    private fun resolveAccountType() = when (kind) {
        CreateAccountModule.Kind.Mnemonic12 -> mnemonicAccountType(12)
        CreateAccountModule.Kind.Mnemonic24 -> mnemonicAccountType(24)
    }

    private fun mnemonicAccountType(wordCount: Int): AccountType {
        val words = wordsManager.generateWords(wordCount)
        return AccountType.Mnemonic(words, passphrase)
    }

    fun validatePassphrase(text: String?): Boolean {
        return passphraseValidator.validate(text)
    }

    sealed class CreateError : Throwable() {
        object EmptyPassphrase : CreateError()
        object InvalidConfirmation : CreateError()
    }

}
