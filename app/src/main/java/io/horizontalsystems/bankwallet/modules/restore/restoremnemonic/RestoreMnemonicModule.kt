package io.horizontalsystems.bankwallet.modules.restore.restoremnemonic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.AccountType

object RestoreMnemonicModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RestoreMnemonicViewModel(
                App.accountFactory,
                App.wordsManager,
                App.thirdKeyboardStorage,
                RestoreMnemonicHelper()
            ) as T
        }
    }

    data class UiState(
        val passphraseEnabled: Boolean = false,
        val passphrase: String = "",
        val passphraseError: String? = null,
        val words: List<WordItem> = listOf(),
        val invalidWords: List<WordItem> = listOf(),
        val invalidWordRanges: List<IntRange> = listOf(),
        val error: String? = null,
        val accountType: AccountType? = null,
        val wordSuggestions: WordSuggestions? = null,
    )

    data class WordItem(val word: String, val range: IntRange)
    data class State(val allItems: List<WordItem>, val invalidItems: List<WordItem>)
    data class WordSuggestions(val wordItem: WordItem, val options: List<String>)

}
