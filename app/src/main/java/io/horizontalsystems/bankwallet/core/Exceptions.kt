package io.horizontalsystems.bankwallet.core

class UnsupportedAccountException : Exception()
class EosUnsupportedException : Exception()
class WrongParameters : Exception()
class CoinException(val errorTextRes: Int?, val nonTranslatableText: String? = null) : Exception()
