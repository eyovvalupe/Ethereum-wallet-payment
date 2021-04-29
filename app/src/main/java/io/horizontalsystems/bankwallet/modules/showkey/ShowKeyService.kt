package io.horizontalsystems.bankwallet.modules.showkey

import io.horizontalsystems.bankwallet.core.managers.EthereumKitManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.toHexString
import io.horizontalsystems.ethereumkit.core.EthereumKit

class ShowKeyService(
        account: Account,
        private val pinComponent: IPinComponent,
        private val ethereumKitManager: EthereumKitManager
) {
    val words: List<String>
    val passphrase: String

    init {
        if (account.type is AccountType.Mnemonic) {
            words = account.type.words
            passphrase = account.type.passphrase ?: ""
        } else {
            words = listOf()
            passphrase = ""
        }
    }

    val isPinSet: Boolean
        get() = pinComponent.isPinSet

    val evmPrivateKey: String
        get() = EthereumKit.privateKey(words, passphrase, ethereumKitManager.networkType).toByteArray().toHexString()

}
