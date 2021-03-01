package io.horizontalsystems.bankwallet.modules.settings.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.languageswitcher.LanguageSettingsFragment
import io.horizontalsystems.views.ListPosition
import kotlinx.android.synthetic.main.fragment_settings.*

class MainSettingsFragment : BaseFragment() {

    private val presenter by viewModels<MainSettingsPresenter> { MainSettingsModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToRouterEvents(presenter.router as MainSettingsRouter)

        val manageKeys = SettingsMenuItem(R.string.SettingsSecurity_ManageKeys, R.drawable.ic_wallet_20, listPosition = ListPosition.First) {
            presenter.didTapManageKeys()
        }
        val privacySettings = SettingsMenuItem(R.string.Settings_SecurityCenter, R.drawable.ic_security, listPosition = ListPosition.Last) {
            presenter.didTapSecurity()
        }
        val walletConnect = SettingsMenuItem(R.string.Settings_WalletConnect, R.drawable.ic_wallet_connect_20, listPosition = ListPosition.Single) {
            presenter.didTapWalletConnect()
        }
        val notifications = SettingsMenuItem(R.string.Settings_Notifications, R.drawable.ic_notification_20, listPosition = ListPosition.First) {
            presenter.didTapNotifications()
        }
        val baseCurrency = SettingsMenuItem(R.string.Settings_BaseCurrency, R.drawable.ic_currency, listPosition = ListPosition.Middle) {
            presenter.didTapBaseCurrency()
        }
        val language = SettingsMenuItem(R.string.Settings_Language, R.drawable.ic_language, listPosition = ListPosition.Middle) {
            presenter.didTapLanguage()
        }
        val lightMode = SettingsMenuSwitch(R.string.Settings_LightMode, R.drawable.ic_light_mode, listPosition = ListPosition.Middle) {
            presenter.didSwitchLightMode(it)
        }
        val experimentalFeatures = SettingsMenuItem(R.string.Settings_ExperimentalFeatures, R.drawable.ic_experimental, listPosition = ListPosition.Last) {
            presenter.didTapExperimentalFeatures()
        }
        val faq = SettingsMenuItem(R.string.Settings_Faq, R.drawable.ic_faq_20, listPosition = ListPosition.First) {
            presenter.didTapFaq()
        }
        val academy = SettingsMenuItem(R.string.Guides_Title, R.drawable.ic_academy_20, listPosition = ListPosition.Last) {
            presenter.didTapAcademy()
        }
        val twitter = SettingsMenuItem(R.string.Settings_Twitter, R.drawable.ic_twitter, listPosition = ListPosition.First) {
            presenter.didTapTwitter()
        }
        val telegram = SettingsMenuItem(R.string.Settings_Telegram, R.drawable.ic_telegram, listPosition = ListPosition.Middle) {
            presenter.didTapTelegram()
        }
        val reddit = SettingsMenuItem(R.string.Settings_Reddit, R.drawable.ic_reddit, listPosition = ListPosition.Last) {
            presenter.didTapReddit()
        }
        val aboutApp = SettingsMenuItem(R.string.SettingsAboutApp_Title, R.drawable.ic_about_app_20, listPosition = ListPosition.Single) {
            presenter.didTapAboutApp()
        }
        val settingsBottom = SettingsMenuBottom {
            presenter.didTapCompanyLogo()
        }

        val presenterView = presenter.view as MainSettingsView
        val mainSettingsAdapter = MainSettingsAdapter(listOf(
                manageKeys,
                privacySettings,
                null,
                walletConnect,
                null,
                notifications,
                baseCurrency,
                language,
                lightMode,
                experimentalFeatures,
                null,
                faq,
                academy,
                null,
                twitter,
                telegram,
                reddit,
                null,
                aboutApp,
                settingsBottom
        ))

        settingsRecyclerView.adapter = mainSettingsAdapter
        settingsRecyclerView.setHasFixedSize(true)
        settingsRecyclerView.setItemAnimator(null)

        presenterView.baseCurrency.observe(viewLifecycleOwner, { currency ->
            baseCurrency.value = currency
            mainSettingsAdapter.notifyChanged(baseCurrency)
        })

        presenterView.backedUp.observe(viewLifecycleOwner, { wordListBackedUp ->
            manageKeys.attention = !wordListBackedUp
            mainSettingsAdapter.notifyChanged(manageKeys)
        })

        presenterView.pinSet.observe(viewLifecycleOwner, { pinSet ->
            privacySettings.attention = !pinSet
            mainSettingsAdapter.notifyChanged(privacySettings)
        })

        presenterView.language.observe(viewLifecycleOwner, { languageCode ->
            language.value = languageCode
            mainSettingsAdapter.notifyChanged(language)
        })

        presenterView.lightMode.observe(viewLifecycleOwner, { isChecked ->
            lightMode.isChecked = isChecked
            mainSettingsAdapter.notifyChanged(lightMode)
        })

        presenterView.appVersion.observe(viewLifecycleOwner, { version ->
            var appVersion = getString(R.string.Settings_InfoTitleWithVersion, version)
            if (getString(R.string.is_release) == "false") {
                appVersion = "$appVersion (${BuildConfig.VERSION_CODE})"
            }

            settingsBottom.appName = appVersion
            mainSettingsAdapter.notifyChanged(settingsBottom)
        })

        presenterView.termsAccepted.observe(viewLifecycleOwner, { termsAccepted ->
            aboutApp.attention = !termsAccepted
            mainSettingsAdapter.notifyChanged(aboutApp)
        })

        presenterView.walletConnectSessionCount.observe(viewLifecycleOwner, { currency ->
            walletConnect.value = currency
            mainSettingsAdapter.notifyChanged(walletConnect)
        })

        presenter.viewDidLoad()
    }

    override fun onResume() {
        super.onResume()
        subscribeFragmentResult()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        settingsRecyclerView.adapter = null
    }

    private fun subscribeToRouterEvents(router: MainSettingsRouter) {
        router.showManageKeysLiveEvent.observe(this, {
            findNavController().navigate(R.id.mainFragment_to_manageKeysFragment, null, navOptions())
        })

        router.showBaseCurrencySettingsLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(R.id.mainFragment_to_currencySwitcherFragment, null, navOptions())
        })

        router.showLanguageSettingsLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(R.id.mainFragment_to_languageSettingsFragment, null, navOptions())
        })

        router.showAboutLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(R.id.mainFragment_to_aboutAppFragment, null, navOptions())
        })

        router.showNotificationsLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(R.id.mainFragment_to_notificationsFragment, null, navOptions())
        })

        router.openFaqLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(R.id.mainFragment_to_faqListFragment, null, navOptions())
        })

        router.openAcademyLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(R.id.mainFragment_to_academyFragment, null, navOptions())
        })

        router.showSecuritySettingsLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(R.id.mainFragment_to_securitySettingsFragment, null, navOptions())
        })

        router.showExperimentalFeaturesLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(R.id.mainFragment_to_experimentalFeaturesFragment, null, navOptions())
        })

        router.openLinkLiveEvent.observe(viewLifecycleOwner, { link ->
            val uri = Uri.parse(link)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            activity?.startActivity(intent)
        })

        router.reloadAppLiveEvent.observe(viewLifecycleOwner, {
            val nightMode = if (CoreApp.themeStorage.isLightModeOn)
                AppCompatDelegate.MODE_NIGHT_NO else
                AppCompatDelegate.MODE_NIGHT_YES

            AppCompatDelegate.setDefaultNightMode(nightMode)
        })

        router.openWalletConnectLiveEvent.observe(viewLifecycleOwner, {
//            WalletConnectModule.start(null, this, R.id.mainFragment_to_walletConnectMainFragment, navOptions())
            WalletConnectListModule.start(this, navOptions())
        })
    }

    private fun subscribeFragmentResult() {
        getNavigationResult(LanguageSettingsFragment.LANGUAGE_CHANGE)?.let {
            activity?.let { MainModule.startAsNewTask(it, MainActivity.SETTINGS_TAB_POSITION) }
        }
    }
}
