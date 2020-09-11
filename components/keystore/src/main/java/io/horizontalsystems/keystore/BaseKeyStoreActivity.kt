package io.horizontalsystems.keystore

import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricConstants
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import io.horizontalsystems.views.AlertDialogFragment
import kotlinx.android.synthetic.main.activity_keystore.*

abstract class BaseKeyStoreActivity : AppCompatActivity() {

    abstract var viewModel: KeyStoreViewModel

    abstract fun openMainModule()

    fun observeEvents() {
        viewModel.showNoSystemLockWarning.observe(this, Observer {
            noSystemLockWarning.isVisible = true
        })

        viewModel.showInvalidKeyWarning.observe(this, Observer {
            AlertDialogFragment.newInstance(
                    titleString = getString(R.string.Alert_KeysInvalidatedTitle),
                    descriptionString = getString(R.string.Alert_KeysInvalidatedDescription),
                    buttonText = R.string.Alert_Ok,
                    cancelable = false,
                    listener = object : AlertDialogFragment.Listener {
                        override fun onButtonClick() {
                            viewModel.delegate.onCloseInvalidKeyWarning()
                        }

                        override fun onCancel() {
                            finishAffinity()
                        }
                    }).show(supportFragmentManager, "keys_invalidated_alert")
        })

        viewModel.promptUserAuthentication.observe(this, Observer {
            showBiometricPrompt()
        })

        viewModel.openLaunchModule.observe(this, Observer {
            openMainModule()
        })

        viewModel.closeApplication.observe(this, Observer {
            finishAffinity()
        })
    }

    override fun onPause() {
        super.onPause()
        if (noSystemLockWarning.isVisible) {
            finishAffinity()
        }
    }

    private fun showBiometricPrompt() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.OSPin_Confirm_Title))
                .setDescription(getString(R.string.OSPin_Prompt_Desciption))
                .setDeviceCredentialAllowed(true)
                .build()

        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                viewModel.delegate.onAuthenticationSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)

                if (errorCode == BiometricConstants.ERROR_USER_CANCELED
                        || errorCode == BiometricConstants.ERROR_NEGATIVE_BUTTON
                        || errorCode == BiometricConstants.ERROR_CANCELED) {
                    viewModel.delegate.onAuthenticationCanceled()
                }
            }
        })

        biometricPrompt.authenticate(promptInfo)
    }

}
