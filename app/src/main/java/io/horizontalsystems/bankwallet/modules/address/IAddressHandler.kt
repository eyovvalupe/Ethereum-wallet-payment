package io.horizontalsystems.bankwallet.modules.address

import com.unstoppabledomains.resolution.Resolution
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.binancechainkit.helpers.Crypto
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import io.horizontalsystems.bitcoincore.utils.CashAddressConverter
import io.horizontalsystems.bitcoincore.utils.SegwitAddressConverter
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import org.web3j.ens.EnsResolver

interface IAddressHandler {
    fun isSupported(value: String): Boolean
    fun parseAddress(value: String): Address
}

class AddressHandlerEns(private val ensResolver: EnsResolver) : IAddressHandler {
    private val cache = mutableMapOf<String, Address>()

    override fun isSupported(value: String): Boolean {
        if (!EnsResolver.isValidEnsName(value)) return false

        try {
            cache[value] = Address(ensResolver.resolve(value), value)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    override fun parseAddress(value: String): Address {
        return cache[value]!!
    }
}

class AddressHandlerUdn(private val tokenQuery: TokenQuery, private val coinCode: String?) : IAddressHandler {
//    private val infuraUrl = RpcSource.ethereumInfuraHttp(App.appConfigProvider.infuraProjectId, App.appConfigProvider.infuraProjectSecret).urls.first()
//    private val polygonUrl = RpcSource.polygonRpcHttp().urls.first()
//    private var resolution: DomainResolution = Resolution.builder()
//        .unsProviderUrl(UNSLocation.Layer1, infuraUrl.toString())
//        .unsProviderUrl(UNSLocation.Layer2, polygonUrl.toString())
//        .build()

    private val resolution = Resolution()
    private val chain by lazy { chain(tokenQuery) }
    private val chainCoinCode by lazy { chainCoinCode(tokenQuery.blockchainType) }

    override fun isSupported(value: String): Boolean {
        return value.contains(".") && resolution.isSupported(value)
    }

    override fun parseAddress(value: String): Address {
        return Address(resolveAddress(value), value)
    }

    private fun resolveAddress(value: String): String {
        val fetchers = mutableListOf<() -> String?>()
        chain?.let { chain ->
            coinCode?.let { coinCode ->
                fetchers.add {
                    resolution.getMultiChainAddress(value, coinCode, chain)
                }
            }
        }
        coinCode?.let { coinCode ->
            fetchers.add {
                resolution.getAddress(value, coinCode)
            }
        }
        fetchers.add {
            resolution.getAddress(value, chainCoinCode)
        }

        var lastError: Throwable? = null
        for (fetcher in fetchers) {
            try {
                fetcher.invoke()?.let { resolvedAddress ->
                    return resolvedAddress
                }
            } catch (t: Throwable) {
                lastError = t
            }
        }

        throw lastError!!
    }

    companion object {
        private fun chainCoinCode(blockchainType: BlockchainType) = when (blockchainType) {
            BlockchainType.Ethereum,
            BlockchainType.EthereumGoerli,
            BlockchainType.BinanceSmartChain,
            BlockchainType.BinanceChain,
            BlockchainType.Polygon,
            BlockchainType.Optimism,
            BlockchainType.Avalanche,
            BlockchainType.Gnosis,
            BlockchainType.ArbitrumOne -> "ETH"
            BlockchainType.Bitcoin -> "BTC"
            BlockchainType.BitcoinCash -> "BCH"
            BlockchainType.Litecoin -> "LTC"
            BlockchainType.Dash -> "DASH"
            BlockchainType.Zcash -> "ZEC"
            BlockchainType.Solana -> "SOL"
            is BlockchainType.Unsupported -> blockchainType.uid
        }

        private fun chain(tokenQuery: TokenQuery) = when (tokenQuery.tokenType) {
            TokenType.Native -> when (tokenQuery.blockchainType) {
                BlockchainType.Polygon -> "MATIC"
                else -> null
            }
            is TokenType.Eip20 -> when (tokenQuery.blockchainType) {
                BlockchainType.BinanceSmartChain -> "BEP20"
                BlockchainType.Polygon -> "MATIC"
                BlockchainType.Avalanche -> "AVAX"
                BlockchainType.Ethereum,
                BlockchainType.Optimism,
                BlockchainType.ArbitrumOne,
                BlockchainType.Gnosis -> "ERC20"
                else -> null
            }
            else -> null
        }
    }

}

class AddressHandlerEvm : IAddressHandler {
    override fun isSupported(value: String) = try {
        AddressValidator.validate(value)
        true
    } catch (e: AddressValidator.AddressValidationException) {
        false
    }

    override fun parseAddress(value: String): Address {
        val evmAddress = io.horizontalsystems.ethereumkit.models.Address(value)
        return Address(evmAddress.hex)
    }

}

class AddressHandlerBase58(network: Network) : IAddressHandler {
    private val converter = Base58AddressConverter(network.addressVersion, network.addressScriptVersion)

    override fun isSupported(value: String) = try {
        converter.convert(value)
        true
    } catch (e: Throwable) {
        false
    }

    override fun parseAddress(value: String): Address {
        return Address(converter.convert(value).string)
    }
}

class AddressHandlerBech32(network: Network) : IAddressHandler {
    private val converter = SegwitAddressConverter(network.addressSegwitHrp)

    override fun isSupported(value: String) = try {
        converter.convert(value)
        true
    } catch (e: Throwable) {
        false
    }

    override fun parseAddress(value: String): Address {
        return Address(converter.convert(value).string)
    }
}

class AddressHandlerBitcoinCash(network: Network) : IAddressHandler {
    private val converter = CashAddressConverter(network.addressSegwitHrp)

    override fun isSupported(value: String) = try {
        converter.convert(value)
        true
    } catch (e: Throwable) {
        false
    }

    override fun parseAddress(value: String): Address {
        return Address(converter.convert(value).string)
    }
}

class AddressHandlerBinanceChain : IAddressHandler {
    override fun isSupported(value: String) = try {
        Crypto.decodeAddress(value)
        true
    } catch (e: Throwable) {
        false
    }

    override fun parseAddress(value: String): Address {
        Crypto.decodeAddress(value)
        return Address(value)
    }
}

class AddressHandlerSolana : IAddressHandler {
    override fun isSupported(value: String) = true

    override fun parseAddress(value: String): Address {
        try {
            //simulate steps in Solana kit init
            io.horizontalsystems.solanakit.models.Address(value)
        } catch (e: Throwable) {
            throw AddressValidator.AddressValidationException(e.message ?: "")
        }

        return Address(value)
    }

}

class AddressHandlerPure : IAddressHandler {
    override fun isSupported(value: String) = true

    override fun parseAddress(value: String) = Address(value)

}