package io.horizontalsystems.bankwallet.core.managers

import android.annotation.SuppressLint
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.modules.addtoken.bep2.Bep2Token
import io.reactivex.Flowable
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class NetworkManager : INetworkManager {

    override fun getGuide(host: String, path: String): Single<String> {
        return ServiceGuide.service(host).getGuide(path)
    }

    override fun getTransaction(host: String, path: String, isSafeCall: Boolean): Flowable<JsonObject> {
        return ServiceFullTransaction.service(host, isSafeCall).getFullTransaction(path)
    }

    override fun getTransactionWithPost(host: String, path: String, body: Map<String, Any>): Flowable<JsonObject> {
        return ServiceFullTransaction.service(host)
                .getFullTransactionWithPost(path, body.mapValues { it.value.toString() })
    }

    override fun ping(host: String, url: String, isSafeCall: Boolean): Flowable<Any> {
        return ServicePing.service(host, isSafeCall).ping(url)
    }

    override fun getErc20CoinInfo(host: String, path: String): Flowable<JsonObject> {
        return ServiceErc20ContractInfo.service(host).getTokenInfo(path)
    }

    override fun getBep2Tokens(host: String, path: String): Flowable<List<Bep2Token>> {
        return ServiceBep2Tokens.service(host).getTokens(path)
    }
}

object ServiceFullTransaction {
    fun service(apiURL: String, isSafeCall: Boolean = true): FullTransactionAPI {
        return APIClient.retrofit(apiURL, 60, isSafeCall)
                .create(FullTransactionAPI::class.java)
    }

    interface FullTransactionAPI {
        @GET
        @Headers("Content-Type: application/json")
        fun getFullTransaction(@Url path: String): Flowable<JsonObject>

        @POST
        @Headers("Content-Type: application/json")
        fun getFullTransactionWithPost(@Url path: String, @Body body: Map<String, String>): Flowable<JsonObject>
    }

}

object ServicePing {
    fun service(apiURL: String, isSafeCall: Boolean = true): FullTransactionAPI {
        return APIClient.retrofit(apiURL, timeout = 8, isSafeCall = isSafeCall).create(FullTransactionAPI::class.java)
    }

    interface FullTransactionAPI {
        @GET
        fun ping(@Url path: String): Flowable<Any>
    }
}

object ServiceErc20ContractInfo {
    fun service(apiURL: String): Erc20ContractInfoAPI {
        return APIClient.retrofit(apiURL, 60)
                .create(Erc20ContractInfoAPI::class.java)
    }

    interface Erc20ContractInfoAPI {
        @GET
        @Headers("Content-Type: application/json")
        fun getTokenInfo(@Url path: String): Flowable<JsonObject>
    }

}

object ServiceBep2Tokens {
    fun service(apiURL: String): Bep2Api {
        return APIClient.retrofit(apiURL, 60)
                .create(Bep2Api::class.java)
    }

    interface Bep2Api {
        @GET
        @Headers("Content-Type: application/json")
        fun getTokens(@Url path: String): Flowable<List<Bep2Token>>
    }

}

object ServiceGuide {
    fun service(apiURL: String): GuidesAPI {
        return APIClient.retrofit(apiURL, 60, true).create(GuidesAPI::class.java)
    }

    interface GuidesAPI {
        @GET
        fun getGuide(@Url path: String): Single<String>
    }
}

object APIClient {

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    //share OkHttpClient
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(logger)
            .build()

    fun retrofit(apiURL: String, timeout: Long = 60, isSafeCall: Boolean = true): Retrofit {

        val httpClient = okHttpClient.newBuilder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)

        //TODO Replace this implementation with Manifest file settings when support for SDK 26 removed
        if (!isSafeCall) // if host name cannot be verified, has no or self signed certificate, do unsafe request
            setUnsafeSocketFactory(httpClient)

        val gsonBuilder = GsonBuilder().setLenient()

        return Retrofit.Builder()
                .baseUrl(apiURL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
                .client(httpClient.build())
                .build()
    }

    @SuppressLint("TrustAllX509TrustManager", "BadHostnameVerifier")
    private fun setUnsafeSocketFactory(builder: OkHttpClient.Builder) {
        try {
            val trustAllCerts = arrayOf<TrustManager>(
                    object : X509TrustManager {
                        @Throws(CertificateException::class)
                        override fun checkClientTrusted(chain: Array<X509Certificate>,
                                                        authType: String) {
                        }

                        @Throws(CertificateException::class)
                        override fun checkServerTrusted(chain: Array<X509Certificate>,
                                                        authType: String) {
                        }

                        override fun getAcceptedIssuers(): Array<X509Certificate> {
                            return arrayOf()
                        }
                    }
            )
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory = sslContext.socketFactory
            builder.sslSocketFactory(sslSocketFactory, (trustAllCerts[0] as X509TrustManager))
            builder.hostnameVerifier(HostnameVerifier { _, _ -> true })
            builder.connectTimeout(5000, TimeUnit.MILLISECONDS)
            builder.readTimeout(60000, TimeUnit.MILLISECONDS)

        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
