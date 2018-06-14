package bitcoin.wallet.core

interface ILocalStorage {
    fun saveWords(words: List<String>)
}

interface IMnemonic {
    fun generateWords(): List<String>
}
