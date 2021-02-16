package io.horizontalsystems.bankwallet.modules.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.horizontalsystems.bankwallet.modules.balanceonboarding.BalanceOnboardingContainerFragment
import io.horizontalsystems.bankwallet.modules.market.MarketFragment
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsFragment
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsFragment

class MainViewPagerAdapter(fm: FragmentManager, lifecycle: Lifecycle)
    : FragmentStateAdapter(fm, lifecycle) {

    override fun getItemCount() = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MarketFragment()
            1 -> BalanceOnboardingContainerFragment()
            2 -> TransactionsFragment()
            3 -> MainSettingsFragment()
            else -> throw IllegalStateException()
        }
    }
}
