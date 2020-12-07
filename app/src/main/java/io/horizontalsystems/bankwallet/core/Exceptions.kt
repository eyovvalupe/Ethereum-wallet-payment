package io.horizontalsystems.bankwallet.core

class UnsupportedAccountException : Exception()
class EosUnsupportedException : Exception()
class WrongAccountTypeForThisProvider : Exception()
class LocalizedException(val errorTextRes: Int) : Exception()
class AdapterErrorWrongParameters(override val message: String) : Exception()
class EthereumKitNotCreated() : Exception()
