package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.marketkit.models.PlatformCoin

class ContractCallTransactionRecord(
    transaction: Transaction,
    baseCoin: PlatformCoin,
    source: TransactionSource,
    val contractAddress: String?,
    val method: String?,
    val value: TransactionValue?,
    val internalTransactionEvents: List<TransferEvent>,
    val incomingEip20Events: List<TransferEvent>,
    val outgoingEip20Events: List<TransferEvent>
) : EvmTransactionRecord(transaction, baseCoin, source)
