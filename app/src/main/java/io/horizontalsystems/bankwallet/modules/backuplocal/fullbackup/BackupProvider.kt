package io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IEnabledWalletStorage
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import io.horizontalsystems.bankwallet.core.managers.BaseTokenManager
import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.EncryptDecryptManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmSyncSourceManager
import io.horizontalsystems.bankwallet.core.managers.LanguageManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.core.managers.SolanaRpcSourceManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BtcRestoreMode
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
import io.horizontalsystems.bankwallet.modules.backuplocal.BackupLocalModule
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewType
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewTypeManager
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorManager
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.settings.appearance.AppIconService
import io.horizontalsystems.bankwallet.modules.settings.appearance.LaunchScreenService
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.theme.ThemeService
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.core.toHexString
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class BackupProvider(
    private val localStorage: ILocalStorage,
    private val languageManager: LanguageManager,
    private val walletStorage: IEnabledWalletStorage,
    private val settingsManager: RestoreSettingsManager,
    private val accountManager: IAccountManager,
    private val accountFactory: IAccountFactory,
    private val walletManager: IWalletManager,
    private val restoreSettingsManager: RestoreSettingsManager,
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val marketFavoritesManager: MarketFavoritesManager,
    private val balanceViewTypeManager: BalanceViewTypeManager,
    private val appIconService: AppIconService,
    private val themeService: ThemeService,
    private val chartIndicatorManager: ChartIndicatorManager,
    private val balanceHiddenManager: BalanceHiddenManager,
    private val baseTokenManager: BaseTokenManager,
    private val launchScreenService: LaunchScreenService,
    private val currencyManager: CurrencyManager,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmSyncSourceManager: EvmSyncSourceManager,
    private val solanaRpcSourceManager: SolanaRpcSourceManager,
    private val contactsRepository: ContactsRepository
) {
    private val encryptDecryptManager = EncryptDecryptManager()

    private val gson: Gson by lazy {
        GsonBuilder()
            .disableHtmlEscaping()
            .enableComplexMapKeySerialization()
            .create()
    }

    private fun decrypted(crypto: BackupLocalModule.BackupCrypto, passphrase: String): ByteArray {
        val kdfParams = crypto.kdfparams
        val key = EncryptDecryptManager.getKey(passphrase, kdfParams) ?: throw RestoreException.EncryptionKeyException

        if (EncryptDecryptManager.passwordIsCorrect(crypto.mac, crypto.ciphertext, key)) {
            return encryptDecryptManager.decrypt(crypto.ciphertext, key, crypto.cipherparams.iv)
        } else {
            throw RestoreException.InvalidPasswordException
        }
    }

    @Throws
    fun accountType(backup: BackupLocalModule.WalletBackup, passphrase: String): AccountType {
        val decrypted = decrypted(backup.crypto, passphrase)
        return BackupLocalModule.getAccountTypeFromData(backup.type, decrypted)
    }

    fun restoreCexAccount(accountType: AccountType, accountName: String) {
        val account = accountFactory.account(accountName, accountType, AccountOrigin.Restored, true, true)
        accountManager.save(account)
    }

    fun restoreWalletBackup(
        type: AccountType,
        accountName: String,
        backup: BackupLocalModule.WalletBackup,
        fileBackup: Boolean
    ) {
        val account = if (type.isWatchAccountType) {
            accountFactory.watchAccount(accountName, type, true)
        } else {
            accountFactory.account(accountName, type, AccountOrigin.Restored, backup.manualBackup, fileBackup)
        }
        accountManager.save(account)

        val enabledWalletBackups = backup.enabledWallets ?: listOf()
        val enabledWallets = enabledWalletBackups.map {
            EnabledWallet(
                tokenQueryId = it.tokenQueryId,
                accountId = account.id,
                coinName = it.coinName,
                coinCode = it.coinCode,
                coinDecimals = it.decimals
            )
        }
        walletManager.saveEnabledWallets(enabledWallets)

        enabledWalletBackups.forEach { enabledWalletBackup ->
            TokenQuery.fromId(enabledWalletBackup.tokenQueryId)?.let { tokenQuery ->
                if (!enabledWalletBackup.settings.isNullOrEmpty()) {
                    val restoreSettings = RestoreSettings()
                    enabledWalletBackup.settings.forEach { (restoreSettingType, value) ->
                        restoreSettings[restoreSettingType] = value
                    }
                    restoreSettingsManager.save(restoreSettings, account, tokenQuery.blockchainType)
                }
            }
        }
    }

    private fun importWalletBackups(walletBackups: List<WalletBackup2>, passphrase: String) {
        val accounts = mutableListOf<Account>()
        val enabledWallets = mutableListOf<EnabledWallet>()

        walletBackups.forEach { walletBackup2 ->
            val backup = walletBackup2.backup
            val type = accountType(backup, passphrase)
            val name = walletBackup2.name

            val account = if (type.isWatchAccountType) {
                accountFactory.watchAccount(name, type, true)
            } else if (type is AccountType.Cex) {
                accountFactory.account(name, type, AccountOrigin.Restored, true, true)
            } else {
                accountFactory.account(name, type, AccountOrigin.Restored, backup.manualBackup, backup.fileBackup)
            }
            accounts.add(account)

            val enabledWalletBackups = backup.enabledWallets ?: listOf()
            val wallets = enabledWalletBackups.map {
                EnabledWallet(
                    tokenQueryId = it.tokenQueryId,
                    accountId = account.id,
                    coinName = it.coinName,
                    coinCode = it.coinCode,
                    coinDecimals = it.decimals
                )
            }
            enabledWallets.addAll(wallets)

            enabledWalletBackups.forEach { enabledWalletBackup ->
                TokenQuery.fromId(enabledWalletBackup.tokenQueryId)?.let { tokenQuery ->
                    if (!enabledWalletBackup.settings.isNullOrEmpty()) {
                        val restoreSettings = RestoreSettings()
                        enabledWalletBackup.settings.forEach { (restoreSettingType, value) ->
                            restoreSettings[restoreSettingType] = value
                        }
                        restoreSettingsManager.save(restoreSettings, account, tokenQuery.blockchainType)
                    }
                }
            }
        }

        if (accounts.isNotEmpty()) {
            accountManager.import(accounts)
            walletManager.saveEnabledWallets(enabledWallets)
        }
    }

    @Throws
    suspend fun restoreFullBackup(fullBackup: FullBackup, passphrase: String) {
        fullBackup.wallets?.let { wallets ->
            importWalletBackups(wallets, passphrase)
        }

        fullBackup.watchlist?.let { coinUids ->
            marketFavoritesManager.addAll(coinUids)
        }

        fullBackup.settings.let { settings ->
            balanceViewTypeManager.setViewType(settings.balanceViewType)

            withContext(Dispatchers.Main) {
                try {
                    themeService.setThemeType(settings.currentTheme)
                    languageManager.currentLocaleTag = settings.language
                } catch (e: Exception) {
                    Log.e("ee", "error restore", e)
                }
            }

            if (settings.chartIndicatorsEnabled) {
                chartIndicatorManager.enable()
            } else {
                chartIndicatorManager.disable()
            }

            balanceHiddenManager.setBalanceAutoHidden(settings.balanceAutoHidden)

            settings.conversionTokenQueryId?.let { baseTokenManager.setBaseTokenQueryId(it) }

            settings.swapProviders.forEach {
                localStorage.setSwapProviderId(BlockchainType.fromUid(it.blockchainTypeId), it.provider)
            }

            launchScreenService.setLaunchScreen(settings.launchScreen)
            localStorage.marketsTabEnabled = settings.marketsTabEnabled
            currencyManager.setBaseCurrencyCode(settings.baseCurrency)
            localStorage.isLockTimeEnabled = settings.lockTimeEnabled


            settings.btcModes.forEach { btcMode ->
                val blockchainType = BlockchainType.fromUid(btcMode.blockchainTypeId)

                val restoreMode = BtcRestoreMode.values().firstOrNull { it.raw == btcMode.restoreMode }
                restoreMode?.let { btcBlockchainManager.save(it, blockchainType) }

                val sortMode = TransactionDataSortMode.values().firstOrNull { it.raw == btcMode.sortMode }
                sortMode?.let { btcBlockchainManager.save(sortMode, blockchainType) }
            }

            settings.evmSyncSources.custom.forEach { syncSource ->
                val blockchainType = BlockchainType.fromUid(syncSource.blockchainTypeId)
                val auth = syncSource.auth?.let {
                    val decryptedAuth = decrypted(it, passphrase)
                    String(decryptedAuth, Charsets.UTF_8)
                }
                evmSyncSourceManager.saveSyncSource(blockchainType, syncSource.url, auth)
            }

            settings.evmSyncSources.selected.forEach { syncSource ->
                val blockchainType = BlockchainType.fromUid(syncSource.blockchainTypeId)
                blockchainSettingsStorage.save(syncSource.url, blockchainType)
            }

            blockchainSettingsStorage.save(settings.solanaSyncSource.name, BlockchainType.Solana)

//            Log.e("ee", "appIcon: title=${settings.appIcon}, enum=${AppIcon.fromTitle(settings.appIcon)}")
//            AppIcon.fromTitle(settings.appIcon)?.let { appIconService.setAppIcon(it) }
        }

        fullBackup.contacts?.let {
            val decrypted = decrypted(it, passphrase)
            val contactsBackupJson = String(decrypted, Charsets.UTF_8)

            contactsRepository.restore(contactsBackupJson)
        }
    }

    fun fullBackupItems(): BackupItems {
        val accounts = accountManager.accounts
        val wallets = accounts.filter { !it.isWatchAccount }.sortedBy { it.name.lowercase() }

        val otherBackupItems = buildList {
            val watchWallets = accounts.filter { it.isWatchAccount }
            if (watchWallets.isNotEmpty()) {
                add(
                    BackupItem(
                        title = Translator.getString(R.string.BackupManager_WatchWallets),
                        subtitle = Translator.getString(R.string.BackupManager_Addresses, watchWallets.size),
                    )
                )
            }

            val watchlist = marketFavoritesManager.getAll()
            if (watchlist.isNotEmpty()) {
                add(
                    BackupItem(
                        title = Translator.getString(R.string.BackupManager_WatchlistTitle),
                        subtitle = Translator.getString(R.string.BackupManager_WatchlistDescription),
                    )
                )
            }

            val contacts = contactsRepository.contacts
            if (contacts.isNotEmpty()) {
                add(
                    BackupItem(
                        title = Translator.getString(R.string.Contacts),
                        subtitle = Translator.getString(R.string.BackupManager_Addresses, contacts.size)
                    )
                )
            }

            add(
                BackupItem(
                    title = Translator.getString(R.string.BlockchainSettings_Title),
                    subtitle = Translator.getString(R.string.BackupManager_BlockchainSettingsDescription)
                )
            )

            add(
                BackupItem(
                    title = Translator.getString(R.string.Info_LockTime_Title),
                    subtitle = if (localStorage.isLockTimeEnabled)
                        Translator.getString(R.string.BackupManager_Enabled)
                    else
                        Translator.getString(R.string.BackupManager_Disabled)
                )
            )

            add(
                BackupItem(
                    title = Translator.getString(R.string.CoinPage_Indicators),
                    subtitle = "MA, MACD, RSI"
                )
            )

            add(
                BackupItem(
                    title = Translator.getString(R.string.BackupManager_LanguageAndCurrency),
                    subtitle = "${languageManager.currentLanguageName}, ${currencyManager.baseCurrency.code}"
                )
            )

            add(
                BackupItem(
                    title = Translator.getString(R.string.Settings_Appearance),
                    subtitle = Translator.getString(R.string.BackupManager_AppearanceSettingsDescription)
                )
            )
        }

        return BackupItems(wallets, otherBackupItems)
    }

    @Throws
    fun createWalletBackup(account: Account, passphrase: String): String {
        val backup = walletBackup(account, passphrase)
        return gson.toJson(backup)
    }

    @Throws
    fun createFullBackup(accountIds: List<String>, passphrase: String): String {
        Log.e("eee", "fullBackupJson() accountIds ${accountIds.size}: ${accountIds.joinToString(separator = ",")}")

        accountIds.forEach {
            Log.e("eee", "separate print accountId: $it")
        }

        Log.e(
            "eee",
            "fullBackupJson() selectedAccounts = ${
                accountManager.accounts.filter { it.isWatchAccount || accountIds.contains(it.id) }.joinToString(separator = ",") { it.name }
            }"
        )
        val wallets = accountManager.accounts
            .filter { it.isWatchAccount || accountIds.contains(it.id) }
            .map {
                val accountBackup = walletBackup(it, passphrase)
                WalletBackup2(it.name, accountBackup)
            }

        val watchlist = marketFavoritesManager.getAll().map { it.coinUid }

        val swapProviders = evmBlockchainManager.allBlockchainTypes.map { blockchainType ->
            val provider = localStorage.getSwapProviderId(blockchainType) ?: SwapMainModule.OneInchProvider.id
            SwapProvider(blockchainType.uid, provider)
        }

        val btcModes = btcBlockchainManager.allBlockchains.map { blockchain ->
            val restoreMode = btcBlockchainManager.restoreMode(blockchain.type)
            val sortMode = btcBlockchainManager.transactionSortMode(blockchain.type)
            BtcMode(blockchain.uid, restoreMode.raw, sortMode.raw)
        }

        val selectedEvmSyncSources = evmBlockchainManager.allBlockchains.map { blockchain ->
            val syncSource = evmSyncSourceManager.getSyncSource(blockchain.type)
            EvmSyncSourceBackup(blockchain.uid, syncSource.uri.toString(), null)
        }

        val customEvmSyncSources = evmBlockchainManager.allBlockchains.map { blockchain ->
            val customEvmSyncSources = evmSyncSourceManager.customSyncSources(blockchain.type)
            customEvmSyncSources.map { syncSource ->
                val auth = syncSource.auth?.let { encrypted(it, passphrase) }
                EvmSyncSourceBackup(blockchain.uid, syncSource.uri.toString(), auth)
            }
        }.flatten()

        val evmSyncSources = EvmSyncSources(selected = selectedEvmSyncSources, custom = customEvmSyncSources)

        val solanaSyncSource = SolanaSyncSource(BlockchainType.Solana.uid, solanaRpcSourceManager.rpcSource.name)

        val settings = Settings(
            balanceViewType = balanceViewTypeManager.balanceViewTypeFlow.value,
            appIcon = appIconService.optionsFlow.value.selected.titleText,
            currentTheme = themeService.optionsFlow.value.selected,
            chartIndicatorsEnabled = chartIndicatorManager.isEnabledFlow.value,
            balanceAutoHidden = balanceHiddenManager.balanceAutoHidden,
            conversionTokenQueryId = baseTokenManager.token?.tokenQuery?.id,
            swapProviders = swapProviders,
            language = languageManager.currentLocaleTag,
            launchScreen = launchScreenService.optionsFlow.value.selected,
            marketsTabEnabled = localStorage.marketsTabEnabled,
            baseCurrency = currencyManager.baseCurrency.code,
            lockTimeEnabled = localStorage.isLockTimeEnabled,
            btcModes = btcModes,
            evmSyncSources = evmSyncSources,
            solanaSyncSource = solanaSyncSource,
        )

        val contacts = if (contactsRepository.contacts.isNotEmpty())
            encrypted(contactsRepository.asJsonString, passphrase)
        else
            null

        val fullBackup = FullBackup(
            wallets = wallets.ifEmpty { null },
            watchlist = watchlist.ifEmpty { null },
            settings = settings,
            contacts = contacts,
            timestamp = System.currentTimeMillis() / 1000,
        )

        return gson.toJson(fullBackup)
    }

    private fun getId(value: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-512")
        val digest = md.digest(value)
        return digest.toHexString()
    }

    private fun encrypted(data: String, passphrase: String): BackupLocalModule.BackupCrypto {
        val kdfParams = BackupLocalModule.kdfDefault
        val secretText = data.toByteArray(Charsets.UTF_8)
        val key = EncryptDecryptManager.getKey(passphrase, kdfParams) ?: throw Exception("Couldn't get encryption key")

        val iv = EncryptDecryptManager.generateRandomBytes(16).toHexString()
        val encrypted = encryptDecryptManager.encrypt(secretText, key, iv)
        val mac = EncryptDecryptManager.generateMac(key, encrypted.toByteArray())

        return BackupLocalModule.BackupCrypto(
            cipher = "aes-128-ctr",
            cipherparams = BackupLocalModule.CipherParams(iv),
            ciphertext = encrypted,
            kdf = "scrypt",
            kdfparams = kdfParams,
            mac = mac.toHexString()
        )
    }

    @Throws
    private fun walletBackup(account: Account, passphrase: String): BackupLocalModule.WalletBackup {
        val kdfParams = BackupLocalModule.kdfDefault
        val secretText = BackupLocalModule.getDataForEncryption(account.type)
        val id = getId(secretText)
        val key = EncryptDecryptManager.getKey(passphrase, kdfParams) ?: throw Exception("Couldn't get encryption key")

        val iv = EncryptDecryptManager.generateRandomBytes(16).toHexString()
        val encrypted = encryptDecryptManager.encrypt(secretText, key, iv)
        val mac = EncryptDecryptManager.generateMac(key, encrypted.toByteArray())

        val wallets = walletStorage.enabledWallets(account.id)

        val enabledWalletsBackup = wallets.mapNotNull {
            val tokenQuery = TokenQuery.fromId(it.tokenQueryId) ?: return@mapNotNull null
            val settings = settingsManager.settings(account, tokenQuery.blockchainType).values
            BackupLocalModule.EnabledWalletBackup(
                tokenQueryId = it.tokenQueryId,
                coinName = it.coinName,
                coinCode = it.coinCode,
                decimals = it.coinDecimals,
                settings = settings.ifEmpty { null }
            )
        }

        val crypto = BackupLocalModule.BackupCrypto(
            cipher = "aes-128-ctr",
            cipherparams = BackupLocalModule.CipherParams(iv),
            ciphertext = encrypted,
            kdf = "scrypt",
            kdfparams = kdfParams,
            mac = mac.toHexString()
        )

        return BackupLocalModule.WalletBackup(
            crypto = crypto,
            id = id,
            type = BackupLocalModule.getAccountTypeString(account.type),
            enabledWallets = enabledWalletsBackup,
            manualBackup = account.isBackedUp,
            fileBackup = account.isFileBackedUp,
            timestamp = System.currentTimeMillis() / 1000,
            version = 2
        )
    }

}

data class BackupItem(
    val title: String,
    val subtitle: String
)

data class BackupItems(
    val wallets: List<Account>,
    val others: List<BackupItem>
)

data class WalletBackup2(
    val name: String,
    val backup: BackupLocalModule.WalletBackup
)

data class FullBackup(
    val wallets: List<WalletBackup2>?,
    val watchlist: List<String>?,
    val settings: Settings,
    val contacts: BackupLocalModule.BackupCrypto?,
    val timestamp: Long
)

data class SwapProvider(
    @SerializedName("blockchain_type_id")
    val blockchainTypeId: String,
    @SerializedName("provider")
    val provider: String
)

data class BtcMode(
    @SerializedName("blockchain_type_id")
    val blockchainTypeId: String,
    @SerializedName("restore_mode")
    val restoreMode: String,
    @SerializedName("sort_mode")
    val sortMode: String
)

data class EvmSyncSourceBackup(
    @SerializedName("blockchain_type_id")
    val blockchainTypeId: String,
    val url: String,
    val auth: BackupLocalModule.BackupCrypto?
)

data class EvmSyncSources(
    val selected: List<EvmSyncSourceBackup>,
    val custom: List<EvmSyncSourceBackup>
)

data class SolanaSyncSource(
    @SerializedName("blockchain_type_id")
    val blockchainTypeId: String,
    val name: String
)

data class Settings(
    @SerializedName("balance_primary_value")
    val balanceViewType: BalanceViewType,
    @SerializedName("app_icon")
    val appIcon: String,
    @SerializedName("theme_mode")
    val currentTheme: ThemeType,
    @SerializedName("indicators_shown")
    val chartIndicatorsEnabled: Boolean,
    @SerializedName("balance_auto_hide")
    val balanceAutoHidden: Boolean,
    @SerializedName("conversion_token_query_id")
    val conversionTokenQueryId: String?,
    @SerializedName("swap_providers")
    val swapProviders: List<SwapProvider>,
    val language: String,
    @SerializedName("launch_screen")
    val launchScreen: LaunchPage,
    @SerializedName("show_market")
    val marketsTabEnabled: Boolean,
    @SerializedName("currency")
    val baseCurrency: String,
    @SerializedName("lock_time")
    val lockTimeEnabled: Boolean,

    @SerializedName("btc_modes")
    val btcModes: List<BtcMode>,
    @SerializedName("evm_sync_sources")
    val evmSyncSources: EvmSyncSources,
    @SerializedName("solana_sync_source")
    val solanaSyncSource: SolanaSyncSource
)

sealed class RestoreException(message: String) : Exception(message) {
    object EncryptionKeyException : RestoreException("Couldn't get key from passphrase.")
    object InvalidPasswordException : RestoreException(Translator.getString(R.string.ImportBackupFile_Error_InvalidPassword))
}
