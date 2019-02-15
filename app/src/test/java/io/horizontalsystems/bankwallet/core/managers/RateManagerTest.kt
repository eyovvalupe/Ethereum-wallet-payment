package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.atMost
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.entities.LatestRate
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.Flowable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class RateManagerTest {

    private lateinit var rateManager: RateManager

    private val networkManager = mock(INetworkManager::class.java)

    private val storage = mock(IRateStorage::class.java)

    @Before
    fun setup() {
        RxBaseTest.setup()

        rateManager = RateManager(storage, networkManager)
    }

    @Test
    fun refreshRates() {
        val coins = listOf("BTC", "ETH")
        val currencyCode = "USD"

        whenever(networkManager.getLatestRate(coins[0], currencyCode)).thenReturn(Flowable.just(LatestRate(123.12.toBigDecimal(), 1000)))
        whenever(networkManager.getLatestRate(coins[1], currencyCode)).thenReturn(Flowable.just(LatestRate(456.45.toBigDecimal(), 2000)))

        rateManager.refreshLatestRates(coins, currencyCode)

        verify(storage).saveLatest(Rate(coins[0], currencyCode, 123.12.toBigDecimal(), 1000, true))
        verify(storage).saveLatest(Rate(coins[1], currencyCode, 456.45.toBigDecimal(), 2000, true))
        verify(storage, atMost(2)).saveLatest(any())
    }

    @Test
    fun refreshRates_oneEmpty() {
        val coins = listOf("BTC", "ETH")
        val currencyCode = "USD"

        whenever(networkManager.getLatestRate(coins[0], currencyCode)).thenReturn(Flowable.empty())
        whenever(networkManager.getLatestRate(coins[1], currencyCode)).thenReturn(Flowable.just(LatestRate(456.45.toBigDecimal(), 2000)))

        rateManager.refreshLatestRates(coins, currencyCode)

        verify(storage).saveLatest(Rate(coins[1], currencyCode, 456.45.toBigDecimal(), 2000, true))
        verify(storage, atMost(1)).saveLatest(any())
    }

    @Test
    fun refreshRates_oneError() {
        val coins = listOf("BTC", "ETH")
        val currencyCode = "USD"

        whenever(networkManager.getLatestRate(coins[0], currencyCode)).thenReturn(Flowable.error(Exception()))
        whenever(networkManager.getLatestRate(coins[1], currencyCode)).thenReturn(Flowable.just(LatestRate(456.45.toBigDecimal(), 2000)))

        rateManager.refreshLatestRates(coins, currencyCode)

        verify(storage).saveLatest(Rate(coins[1], currencyCode, 456.45.toBigDecimal(), 2000, true))
        verify(storage, atMost(1)).saveLatest(any())
    }

    @Test
    fun rateValueObservable() {
        val coinCode = "BTC"
        val currencyCode = "USD"
        val timestamp = 23412L
        val rate = mock(Rate::class.java)
        val rateValue = 123.23.toBigDecimal()

        whenever(rate.value).thenReturn(rateValue)
        whenever(storage.rateObservable(coinCode, currencyCode, timestamp)).thenReturn(Flowable.just(listOf(rate)))

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
                .assertValue(rateValue)

    }

    @Test
    fun rateValueObservable_zeroValue() {
        val coinCode = "BTC"
        val currencyCode = "USD"
        val timestamp = 23412L
        val rate = mock(Rate::class.java)
        val rateValue = 0.toBigDecimal()

        whenever(rate.value).thenReturn(rateValue)
        whenever(storage.rateObservable(coinCode, currencyCode, timestamp)).thenReturn(Flowable.just(listOf(rate)))

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
                .assertNoValues()
    }

    @Test
    fun rateValueObservable_noRate() {
        val coinCode = "BTC"
        val currencyCode = "USD"
        val timestamp = 23412L
        val rateValueFromNetwork = 123.2300.toBigDecimal()

        whenever(storage.rateObservable(coinCode, currencyCode, timestamp)).thenReturn(Flowable.just(listOf()))
        whenever(networkManager.getRate(coinCode, currencyCode, timestamp)).thenReturn(Flowable.just(rateValueFromNetwork))

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
                .assertNoValues()

        verify(storage).save(Rate(coinCode, currencyCode, 0.toBigDecimal(), timestamp, false))
        verify(networkManager).getRate(coinCode, currencyCode, timestamp)
        verify(storage).save(Rate(coinCode, currencyCode, rateValueFromNetwork, timestamp, false))
    }

    @Test
    fun rateValueObservable_noRate_latestRateFallback_earlierThen1Hour() {
        val coinCode = "BTC"
        val currencyCode = "USD"
        val timestamp = ((System.currentTimeMillis() / 1000) - 3600) - 1
        val rateValueFromNetwork = 123.2300.toBigDecimal()
        val latestRate = mock(Rate::class.java)
        val rateValue = 234.23.toBigDecimal()

        whenever(storage.rateObservable(coinCode, currencyCode, timestamp)).thenReturn(Flowable.just(listOf()))
        whenever(storage.latestRateObservable(coinCode, currencyCode)).thenReturn(Flowable.just(latestRate))
        whenever(networkManager.getRate(coinCode, currencyCode, timestamp)).thenReturn(Flowable.just(rateValueFromNetwork))
        whenever(latestRate.expired).thenReturn(false)
        whenever(latestRate.value).thenReturn(rateValue)

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
                .assertNoValues()
    }

    @Test
    fun rateValueObservable_noRate_latestRateFallback_notEarlierThen1Hour_notExpired() {
        val coinCode = "BTC"
        val currencyCode = "USD"
        val timestamp = ((System.currentTimeMillis() / 1000) - 3600) + 1
        val rateValueFromNetwork = 123.2300.toBigDecimal()
        val latestRate = mock(Rate::class.java)
        val rateValue = 234.23.toBigDecimal()

        whenever(storage.rateObservable(coinCode, currencyCode, timestamp)).thenReturn(Flowable.just(listOf()))
        whenever(storage.latestRateObservable(coinCode, currencyCode)).thenReturn(Flowable.just(latestRate))
        whenever(networkManager.getRate(coinCode, currencyCode, timestamp)).thenReturn(Flowable.just(rateValueFromNetwork))
        whenever(latestRate.expired).thenReturn(false)
        whenever(latestRate.value).thenReturn(rateValue)

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
                .assertValue(rateValue)
    }

    @Test
    fun rateValueObservable_noRate_latestRateFallback_notEarlierThen1Hour_expired() {
        val coinCode = "BTC"
        val currencyCode = "USD"
        val timestamp = ((System.currentTimeMillis() / 1000) - 3600) + 1
        val rateValueFromNetwork = 123.2300.toBigDecimal()
        val latestRate = mock(Rate::class.java)

        whenever(storage.rateObservable(coinCode, currencyCode, timestamp)).thenReturn(Flowable.just(listOf()))
        whenever(storage.latestRateObservable(coinCode, currencyCode)).thenReturn(Flowable.just(latestRate))
        whenever(networkManager.getRate(coinCode, currencyCode, timestamp)).thenReturn(Flowable.just(rateValueFromNetwork))
        whenever(latestRate.expired).thenReturn(true)

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
                .assertNoValues()
    }

    @Test
    fun rateValueObservable_noRate_latestRateFallback_notEarlierThen1Hour_emptyLatestRate() {
        val coinCode = "BTC"
        val currencyCode = "USD"
        val timestamp = ((System.currentTimeMillis() / 1000) - 3600) + 1
        val rateValueFromNetwork = 123.2300.toBigDecimal()

        whenever(storage.rateObservable(coinCode, currencyCode, timestamp)).thenReturn(Flowable.just(listOf()))
        whenever(storage.latestRateObservable(coinCode, currencyCode)).thenReturn(Flowable.empty())
        whenever(networkManager.getRate(coinCode, currencyCode, timestamp)).thenReturn(Flowable.just(rateValueFromNetwork))

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
                .assertNoValues()
    }

    @Test
    fun refreshZeroRates() {
        val coinCode1 = "BTC"
        val currencyCode = "USD"
        val timestamp1 = 123L
        val fetchedRateValue1 = 123.123.toBigDecimal()

        val coinCode2 = "ETH"
        val timestamp2 = 876L
        val fetchedRateValue2 = 23423.34.toBigDecimal()

        val rate1 = Rate(coinCode1, currencyCode, 0.toBigDecimal(), timestamp1, false)
        val rate2 = Rate(coinCode2, currencyCode, 0.toBigDecimal(), timestamp2, false)

        whenever(storage.zeroRatesObservable(currencyCode)).thenReturn(Single.just(listOf(rate1, rate2)))

        whenever(networkManager.getRate(coinCode1, currencyCode, timestamp1)).thenReturn(Flowable.just(fetchedRateValue1))
        whenever(networkManager.getRate(coinCode2, currencyCode, timestamp2)).thenReturn(Flowable.just(fetchedRateValue2))

        rateManager.refreshZeroRates(currencyCode)

        verify(storage).save(Rate(coinCode1, currencyCode, fetchedRateValue1, timestamp1, false))
        verify(storage).save(Rate(coinCode2, currencyCode, fetchedRateValue2, timestamp2, false))
    }

}