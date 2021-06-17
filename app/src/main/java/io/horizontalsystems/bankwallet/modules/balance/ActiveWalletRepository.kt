package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.AccountSettingManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.Observable

class ActiveWalletRepository(
    private val walletManager: IWalletManager,
    private val accountSettingManager: AccountSettingManager
) : ItemRepository<Wallet> {

    override val itemsObservable: Observable<List<Wallet>> =
        Observable
            .merge(
                Observable.just(Unit),
                walletManager.activeWalletsUpdatedObservable,
                accountSettingManager.ethereumNetworkObservable,
                accountSettingManager.binanceSmartChainNetworkObservable
            )
            .map {
                walletManager.activeWallets
            }

    override fun refresh() = Unit

}
