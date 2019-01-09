package io.horizontalsystems.bankwallet.modules.balance

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class BalancePresenterTest {

    private val interactor = mock(BalanceModule.IInteractor::class.java)
    private val view = mock(BalanceModule.IView::class.java)
    private val router = mock(BalanceModule.IRouter::class.java)
    private val dataSource = mock(BalanceModule.BalanceItemDataSource::class.java)
    private val factory = mock(BalanceViewItemFactory::class.java)

    private lateinit var presenter: BalancePresenter

    @Before
    fun before() {
        presenter = BalancePresenter(interactor, router, dataSource, factory)
        presenter.view = view
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()

        verify(interactor).initWallets()
    }

    @Test
    fun itemsCount() {
        val itemsCount = 123

        whenever(dataSource.count).thenReturn(itemsCount)

        Assert.assertEquals(itemsCount, presenter.itemsCount)
    }

    @Test
    fun getViewItem() {
        val position = 324

        val item = mock(BalanceModule.BalanceItem::class.java)
        val viewItem = mock(BalanceViewItem::class.java)
        val currency = mock(Currency::class.java)

        whenever(dataSource.getItem(position)).thenReturn(item)
        whenever(dataSource.currency).thenReturn(currency)
        whenever(factory.createViewItem(item, currency)).thenReturn(viewItem)

        Assert.assertEquals(viewItem, presenter.getViewItem(position))
    }

    @Test
    fun getHeaderViewItem() {
        val items = listOf<BalanceModule.BalanceItem>()
        val viewItem = mock(BalanceHeaderViewItem::class.java)
        val currency = mock(Currency::class.java)

        whenever(dataSource.items).thenReturn(items)
        whenever(dataSource.currency).thenReturn(currency)
        whenever(factory.createHeaderViewItem(items, currency)).thenReturn(viewItem)

        Assert.assertEquals(viewItem, presenter.getHeaderViewItem())
    }

    @Test
    fun didUpdateWallets() {
        val title = "title"
        val coinCode = "coinCode"
        val currencyCode = "currencyCode"
        val currency = mock(Currency::class.java)
        val coinCodes = listOf(coinCode)

        val wallets = listOf(Wallet(title, coinCode, mock(IAdapter::class.java)))

        val items = listOf(BalanceModule.BalanceItem(title, coinCode))

        whenever(currency.code).thenReturn(currencyCode)
        whenever(dataSource.currency).thenReturn(currency)
        whenever(dataSource.coinCodes).thenReturn(coinCodes)

        presenter.didUpdateWallets(wallets)

        verify(dataSource).reset(items)
        verify(interactor).fetchRates(currencyCode, coinCodes)
        verify(view).reload()

    }
    @Test
    fun didUpdateWallets_nullCurrency() {
        whenever(dataSource.currency).thenReturn(null)

        presenter.didUpdateWallets(listOf())

        verify(interactor, never()).fetchRates(any(), any())
    }

    @Test
    fun didUpdateBalance() {
        val coinCode = "coinCode"
        val position = 5
        val balance = 123123.123

        whenever(dataSource.getPosition(coinCode)).thenReturn(position)

        presenter.didUpdateBalance(coinCode, balance)

        verify(dataSource).setBalance(position, balance)
        verify(view).updateItem(position)
        verify(view).updateHeader()
    }

    @Test
    fun didUpdateState() {
        val coinCode = "ABC"
        val position = 5
        val state = AdapterState.Synced

        whenever(dataSource.getPosition(coinCode)).thenReturn(position)

        presenter.didUpdateState(coinCode, state)

        verify(dataSource).setState(position, state)
        verify(view).updateItem(position)
        verify(view).updateHeader()
    }

    @Test
    fun didUpdateCurrency() {
        val currencyCode = "USD"
        val currency = mock(Currency::class.java)
        val coinCodes = listOf<CoinCode>()

        whenever(currency.code).thenReturn(currencyCode)
        whenever(dataSource.coinCodes).thenReturn(coinCodes)

        presenter.didUpdateCurrency(currency)

        verify(dataSource).currency = currency
        verify(dataSource).clearRates()
        verify(interactor).fetchRates(currencyCode, coinCodes)
        verify(view).reload()
    }

    @Test
    fun didUpdateRate() {
        val coinCode = "ABC"
        val position = 5
        val rate = mock(Rate::class.java)

        whenever(rate.coinCode).thenReturn(coinCode)
        whenever(dataSource.getPosition(coinCode)).thenReturn(position)

        presenter.didUpdateRate(rate)

        verify(dataSource).setRate(position, rate)
        verify(view).updateItem(position)
        verify(view).updateHeader()
    }

    @Test
    fun onReceive() {
        val position = 5
        val coinCode = "coinCode"
        val item = mock(BalanceModule.BalanceItem::class.java)

        whenever(dataSource.getItem(position)).thenReturn(item)
        whenever(item.coinCode).thenReturn(coinCode)

        presenter.onReceive(position)

        verify(router).openReceiveDialog(coinCode)
    }

    @Test
    fun onPay() {
        val position = 5
        val coinCode = "coinCode"
        val item = mock(BalanceModule.BalanceItem::class.java)

        whenever(dataSource.getItem(position)).thenReturn(item)
        whenever(item.coinCode).thenReturn(coinCode)

        presenter.onPay(position)

        verify(router).openSendDialog(coinCode)
    }
}
