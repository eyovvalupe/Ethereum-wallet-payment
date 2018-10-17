package bitcoin.wallet.core

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import bitcoin.wallet.core.managers.BackgroundManager
import bitcoin.wallet.kit.WalletKit
import com.squareup.leakcanary.LeakCanary

class App : Application() {

    companion object {

        lateinit var preferences: SharedPreferences

        val testMode = true

        var promptPin = true

        var appBackgroundedTime: Long? = null

        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        LeakCanary.install(this)

        // Start WalletKit
        WalletKit.init(this)

        BackgroundManager.init(this)

        instance = this
        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

}
