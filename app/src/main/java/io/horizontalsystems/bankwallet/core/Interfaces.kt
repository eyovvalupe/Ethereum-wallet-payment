package io.horizontalsystems.bankwallet.core

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.core.factories.PriceAlertItem
import io.horizontalsystems.bankwallet.core.managers.TorManager
import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.balance.BalanceSortType
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.core.entities.AppVersion
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.eoskit.EosKit
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.xrateskit.entities.*
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.Subject
import java.math.BigDecimal
import java.util.*

interface IAdapterManager {
    val adaptersReadyObservable: Flowable<Unit>
    fun preloadAdapters()
    fun refresh()
    fun stopKits()
    fun getAdapterForWallet(wallet: Wallet): IAdapter?
    fun getTransactionsAdapterForWallet(wallet: Wallet): ITransactionsAdapter?
    fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter?
    fun getReceiveAdapterForWallet(wallet: Wallet): IReceiveAdapter?
    fun refreshAdapters(wallets: List<Wallet>)
    fun refreshByWallet(wallet: Wallet)
}

interface ILocalStorage {
    var sendInputType: SendModule.InputType?
    var baseCurrencyCode: String?

    var baseBitcoinProvider: String?
    var baseLitecoinProvider: String?
    var baseEthereumProvider: String?
    var baseDashProvider: String?
    var baseBinanceProvider: String?
    var baseEosProvider: String?
    var syncMode: SyncMode?
    var sortType: BalanceSortType
    var appVersions: List<AppVersion>
    var isAlertNotificationOn: Boolean
    var isLockTimeEnabled: Boolean
    var encryptedSampleText: String?
    var bitcoinDerivation: AccountType.Derivation?
    var torEnabled: Boolean
    var appLaunchCount: Int
    var rateAppLastRequestTime: Long
    var transactionSortingType: TransactionDataSortingType
    var balanceHidden: Boolean

    fun clear()
}

interface IChartTypeStorage {
    var chartType: ChartType?
}

interface IAccountManager {
    val isAccountsEmpty: Boolean
    val accounts: List<Account>
    val accountsFlowable: Flowable<List<Account>>
    val accountsDeletedFlowable: Flowable<Unit>

    fun account(coinType: CoinType): Account?
    fun loadAccounts()
    fun save(account: Account)
    fun update(account: Account)
    fun delete(id: String)
    fun clear()
    fun clearAccounts()
}

interface IBackupManager {
    val allBackedUp: Boolean
    val allBackedUpFlowable: Flowable<Boolean>
    fun setIsBackedUp(id: String)
}

interface IAccountCreator {
    fun newAccount(predefinedAccountType: PredefinedAccountType): Account
    fun restoredAccount(accountType: AccountType): Account
}

interface IAccountFactory {
    fun account(type: AccountType, origin: AccountOrigin, backedUp: Boolean): Account
}

interface IWalletStorage {
    fun wallets(accounts: List<Account>): List<Wallet>
    fun enabledCoins(): List<Coin>
    fun save(wallets: List<Wallet>)
    fun delete(wallets: List<Wallet>)
    fun wallet(account: Account, coin: Coin): Wallet?
}

interface IPredefinedAccountTypeManager {
    val allTypes: List<PredefinedAccountType>
    fun account(predefinedAccountType: PredefinedAccountType): Account?
    fun predefinedAccountType(type: AccountType): PredefinedAccountType?
}

interface IRandomProvider {
    fun getRandomIndexes(count: Int): List<Int>
}

interface INetworkManager {
    fun getTransaction(host: String, path: String, isSafeCall: Boolean): Flowable<JsonObject>
    fun getTransactionWithPost(host: String, path: String, body: Map<String, Any>): Flowable<JsonObject>
    fun ping(host: String, url: String, isSafeCall: Boolean): Flowable<Any>
}

interface IClipboardManager {
    fun copyText(text: String)
    fun getCopiedText(): String
    val hasPrimaryClip: Boolean
}

interface ITransactionDataProviderManager {
    val baseProviderUpdatedSignal: Observable<Unit>

    fun providers(coin: Coin): List<FullTransactionInfoModule.Provider>
    fun baseProvider(coin: Coin): FullTransactionInfoModule.Provider
    fun setBaseProvider(name: String, coin: Coin)

    fun bitcoin(name: String): FullTransactionInfoModule.BitcoinForksProvider
    fun litecoin(name: String): FullTransactionInfoModule.BitcoinForksProvider
    fun dash(name: String): FullTransactionInfoModule.BitcoinForksProvider
    fun bitcoinCash(name: String): FullTransactionInfoModule.BitcoinForksProvider
    fun ethereum(name: String): FullTransactionInfoModule.EthereumForksProvider
    fun binance(name: String): FullTransactionInfoModule.BinanceProvider
    fun eos(name: String): FullTransactionInfoModule.EosProvider
}

interface IWordsManager {
    fun validate(words: List<String>)
    fun generateWords(count: Int = 12): List<String>
}

sealed class AdapterState {
    object Synced : AdapterState()
    data class Syncing(val progress: Int, val lastBlockDate: Date?) : AdapterState()
    data class SearchingTxs(val count: Int) : AdapterState()
    data class NotSynced(val error: Throwable) : AdapterState()
}

interface IEthereumKitManager {
    val ethereumKit: EthereumKit?
    val statusInfo: Map<String, Any>?

    fun ethereumKit(wallet: Wallet, communicationMode: CommunicationMode?): EthereumKit
    fun unlink()
}

interface IEosKitManager {
    val eosKit: EosKit?
    val statusInfo: Map<String, Any>?

    fun eosKit(wallet: Wallet): EosKit
    fun unlink()
}

interface IBinanceKitManager {
    val binanceKit: BinanceChainKit?
    val statusInfo: Map<String, Any>?

    fun binanceKit(wallet: Wallet): BinanceChainKit
    fun unlink()
}

interface ITransactionsAdapter {
    val state: AdapterState
    val stateUpdatedFlowable: Flowable<Unit>

    val confirmationsThreshold: Int
    val lastBlockInfo: LastBlockInfo?
    val lastBlockUpdatedFlowable: Flowable<Unit>

    fun getTransactions(from: TransactionRecord?, limit: Int): Single<List<TransactionRecord>>
    fun getRawTransaction(transactionHash: String): String? = null

    val transactionRecordsFlowable: Flowable<List<TransactionRecord>>
}

interface IBalanceAdapter {
    val state: AdapterState
    val stateUpdatedFlowable: Flowable<Unit>

    val balance: BigDecimal
    val balanceLocked: BigDecimal? get() = null
    val balanceUpdatedFlowable: Flowable<Unit>

}

interface IReceiveAdapter {
    val receiveAddress: String
    fun getReceiveAddressType(wallet: Wallet): String?
}

interface ISendBitcoinAdapter {
    fun availableBalance(feeRate: Long, address: String?, pluginData: Map<Byte, IPluginData>?): BigDecimal
    fun minimumSendAmount(address: String?): BigDecimal
    fun maximumSendAmount(pluginData: Map<Byte, IPluginData>): BigDecimal?
    fun fee(amount: BigDecimal, feeRate: Long, address: String?, pluginData: Map<Byte, IPluginData>?): BigDecimal
    fun validate(address: String, pluginData: Map<Byte, IPluginData>?)
    fun send(amount: BigDecimal, address: String, feeRate: Long, pluginData: Map<Byte, IPluginData>?, transactionSorting: TransactionDataSortingType?): Single<Unit>
}

interface ISendDashAdapter {
    fun availableBalance(address: String?): BigDecimal
    fun minimumSendAmount(address: String?): BigDecimal
    fun fee(amount: BigDecimal, address: String?): BigDecimal
    fun validate(address: String)
    fun send(amount: BigDecimal, address: String): Single<Unit>
}

interface ISendEthereumAdapter {
    val ethereumBalance: BigDecimal
    val minimumRequiredBalance: BigDecimal
    val minimumSendAmount: BigDecimal

    fun availableBalance(gasPrice: Long, gasLimit: Long?): BigDecimal
    fun fee(gasPrice: Long, gasLimit: Long): BigDecimal
    fun validate(address: String)
    fun send(amount: BigDecimal, address: String, gasPrice: Long, gasLimit: Long): Single<Unit>
    fun estimateGasLimit(toAddress: String, value: BigDecimal, gasPrice: Long?): Single<Long>

}

interface ISendBinanceAdapter {
    val availableBalance: BigDecimal
    val availableBinanceBalance: BigDecimal
    val fee: BigDecimal

    fun validate(address: String)
    fun send(amount: BigDecimal, address: String, memo: String?): Single<Unit>
}

interface ISendEosAdapter {
    val availableBalance: BigDecimal

    fun validate(account: String)
    fun send(amount: BigDecimal, account: String, memo: String?): Single<Unit>
}

interface IAdapter {
    fun start()
    fun stop()
    fun refresh()

    val debugInfo: String
}

interface IAppStatusManager {
    val status: Map<String, Any>
}

interface IAppConfigProvider {
    val companyWebPageLink: String
    val appWebPageLink: String
    val reportEmail: String
    val walletHelpTelegramGroup: String
    val ipfsId: String
    val ipfsMainGateway: String
    val ipfsFallbackGateway: String
    val infuraProjectId: String
    val infuraProjectSecret: String
    val etherscanApiKey: String
    val fiatDecimal: Int
    val maxDecimal: Int
    val currencies: List<Currency>
    val featuredCoins: List<Coin>
    val coins: List<Coin>
    val derivationSettings: List<DerivationSetting>
    val syncModeSettings: List<SyncModeSetting>
    val communicationSettings: List<CommunicationSetting>
}

interface IRateStorage {
    fun latestRateObservable(coinCode: CoinCode, currencyCode: String): Flowable<Rate>
    fun latestRate(coinCode: CoinCode, currencyCode: String): Rate?
    fun rateSingle(coinCode: CoinCode, currencyCode: String, timestamp: Long): Single<Rate>
    fun save(rate: Rate)
    fun saveLatest(rate: Rate)
    fun deleteAll()
}

interface IRateManager {
    fun set(coins: List<String>)
    fun marketInfo(coinCode: String, currencyCode: String): MarketInfo?
    fun getLatestRate(coinCode: String, currencyCode: String): BigDecimal?
    fun marketInfoObservable(coinCode: String, currencyCode: String): Observable<MarketInfo>
    fun marketInfoObservable(currencyCode: String): Observable<Map<String, MarketInfo>>
    fun historicalRateCached(coinCode: String, currencyCode: String, timestamp: Long): BigDecimal?
    fun historicalRate(coinCode: String, currencyCode: String, timestamp: Long): Single<BigDecimal>
    fun chartInfo(coinCode: String, currencyCode: String, chartType: ChartType): ChartInfo?
    fun chartInfoObservable(coinCode: String, currencyCode: String, chartType: ChartType): Observable<ChartInfo>
    fun getCryptoNews(coinCode: String): Single<List<CryptoNews>>
    fun getTopMarketList(currency: String): Single<List<TopMarket>>
    fun refresh()
}

interface IAccountsStorage {
    val isAccountsEmpty: Boolean

    fun allAccounts(): List<Account>
    fun save(account: Account)
    fun update(account: Account)
    fun delete(id: String)
    fun getNonBackedUpCount(): Flowable<Int>
    fun clear()
    fun getDeletedAccountIds(): List<String>
    fun clearDeleted()
}

interface IPriceAlertsStorage {
    val priceAlertCount: Int
    fun all(): List<PriceAlert>
    fun save(priceAlerts: List<PriceAlert>)
    fun delete(priceAlerts: List<PriceAlert>)
    fun deleteExcluding(coinCodes: List<String>)
}

interface IEmojiHelper {
    val multiAlerts: String

    fun title(signedState: Int): String
    fun body(signedState: Int): String
}

interface IPriceAlertHandler {
    fun handleAlerts(latestRatesMap: Map<String, BigDecimal?>)
}

interface IBackgroundPriceAlertManager {
    fun fetchRates(): Single<Unit>
    fun onAppLaunch()
}

interface INotificationFactory {
    fun notifications(alertItems: List<PriceAlertItem>): List<AlertNotification>
}

interface INotificationManager {
    val isEnabled: Boolean
    fun show(notifications: List<AlertNotification>)
    fun clear()
}

interface IEnabledWalletStorage {
    val enabledWallets: List<EnabledWallet>
    fun save(enabledWallets: List<EnabledWallet>)
    fun delete(enabledWallets: List<EnabledWallet>)
    fun deleteAll()
}

interface IWalletManager {
    val wallets: List<Wallet>
    val walletsUpdatedObservable: Observable<List<Wallet>>
    fun wallet(coin: Coin): Wallet?

    fun loadWallets()
    fun enable(wallets: List<Wallet>)
    fun save(wallets: List<Wallet>)
    fun delete(wallets: List<Wallet>)
    fun clear()
}

interface IAppNumberFormatter {
    fun format(coinValue: CoinValue, realNumber: Boolean = false): String?
    fun format(value: BigDecimal, coinCode: String, realNumber: Boolean = false): String?
    fun format(value: Double, precision: Int = 8): String
    fun formatFiat(value: BigDecimal, symbol: String, minimumFractionDigits: Int, maximumFractionDigits: Int): String
    fun getSignificantDecimal(value: BigDecimal): Int
    fun format(value: BigDecimal, precision: Int): String?
}

interface IFeeRateProvider {
    fun feeRates(): Single<List<FeeRateInfo>>
}

interface IAddressParser {
    fun parse(paymentAddress: String): AddressData
}

interface IBackgroundRateAlertScheduler {
    fun startPeriodicWorker()
    fun stopPeriodicWorker()
}

interface IBlockchainSettingsManager {
    fun derivationSetting(coinType: CoinType): DerivationSetting?
    fun syncModeSetting(coinType: CoinType): SyncModeSetting?
    fun communicationSetting(coinType: CoinType): CommunicationSetting?

    fun saveSetting(derivationSetting: DerivationSetting)
    fun saveSetting(syncModeSetting: SyncModeSetting)
    fun saveSetting(communicationSetting: CommunicationSetting)

    fun initializeSettingsWithDefault(coinType: CoinType)
    fun initializeSettings(coinType: CoinType)
}

interface IDerivationSettingsManager {
    fun defaultDerivationSetting(coinType: CoinType): DerivationSetting?
    fun derivationSetting(coinType: CoinType): DerivationSetting?
    fun updateSetting(derivationSetting: DerivationSetting)
}

interface ISyncModeSettingsManager {
    fun defaultSyncModeSetting(coinType: CoinType): SyncModeSetting?
    fun syncModeSetting(coinType: CoinType): SyncModeSetting?
    fun updateSetting(syncModeSetting: SyncModeSetting)
}

interface ICommunicationSettingsManager {
    fun defaultCommunicationSetting(coinType: CoinType): CommunicationSetting?
    fun communicationSetting(coinType: CoinType): CommunicationSetting?
    fun updateSetting(communicationSetting: CommunicationSetting)
}

interface IAccountCleaner {
    fun clearAccounts(accountIds: List<String>)
    fun clearAccount(coinType: CoinType, accountId: String)
}

interface IRateCoinMapper {
    fun convert(coinCode: String): String?
    fun unconvert(coinCode: String): String
}

interface ITorManager {
    fun start()
    fun stop(): Single<Boolean>
    fun enableTor()
    fun disableTor()
    fun setListener(listener: TorManager.Listener)
    val isTorEnabled: Boolean
    val isTorNotificationEnabled: Boolean
    val torObservable: Subject<TorStatus>
}

interface IRateAppManager {
    val showRateAppObservable: Observable<Unit>

    fun onBalancePageActive()
    fun onBalancePageInactive()
    fun onAppLaunch()
    fun onAppBecomeActive()
}

sealed class FeeRatePriority {
    object LOW : FeeRatePriority()
    object MEDIUM : FeeRatePriority()
    object HIGH : FeeRatePriority()

    class Custom(val value: Int, val range: IntRange) : FeeRatePriority()
}
