package bitcoin.wallet.core

import io.reactivex.Completable
import io.realm.ObjectServerError
import io.realm.Realm
import io.realm.SyncCredentials
import io.realm.SyncUser

class RealmManager {

    fun createWalletRealm(): Realm {
        val realmURL = "realms://grouvi-wallet.us1a.cloud.realm.io/default"

        val config = SyncUser.current()
                .createConfiguration(realmURL)
                .build()

        return Realm.getInstance(config)
    }

    fun login(jwtToken: String): Completable = Completable.create { emitter ->
        val credentials = SyncCredentials.jwt(jwtToken)
        val authURL = "https://grouvi-wallet.us1a.cloud.realm.io"

        try {
            SyncUser.all().forEach { _, user ->
                user.logOut()
            }
            SyncUser.logIn(credentials, authURL)
            emitter.onComplete()
        } catch (error: ObjectServerError) {
            emitter.onError(error)
        }
    }

}
