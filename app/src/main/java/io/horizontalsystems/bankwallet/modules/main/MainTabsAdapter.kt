package io.horizontalsystems.bankwallet.modules.main

import android.util.SparseArray
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import io.horizontalsystems.bankwallet.modules.balance.BalanceFragment
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsFragment
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsFragment

class MainTabsAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {

    var currentItem = 0

    private val registeredFragments = SparseArray<Fragment>()

    override fun getCount(): Int = 3

    override fun getItem(position: Int): Fragment = when (position) {
        0 -> BalanceFragment()
        1 -> TransactionsFragment()
        else -> MainSettingsFragment()
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as Fragment
        registeredFragments.put(position, fragment)
        return fragment
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        registeredFragments.remove(position)
        super.destroyItem(container, position, `object`)
    }

    fun getTransactionFragment(): TransactionsFragment {
        return registeredFragments[1] as TransactionsFragment
    }

}
