package io.horizontalsystems.bankwallet.core

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsFragment
import io.horizontalsystems.core.getNavigationResult

fun NavController.slideFromRight(@IdRes resId: Int, args: Bundle? = null) {
    val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_from_right)
        .setExitAnim(android.R.anim.fade_out)
        .setPopEnterAnim(android.R.anim.fade_in)
        .setPopExitAnim(R.anim.slide_to_right)
        .build()

    navigate(resId, args, navOptions)
}

fun NavController.slideFromBottom(@IdRes resId: Int, args: Bundle? = null) {
    val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_from_bottom)
        .setExitAnim(android.R.anim.fade_out)
        .setPopEnterAnim(android.R.anim.fade_in)
        .setPopExitAnim(R.anim.slide_to_bottom)
        .build()

    navigate(resId, args, navOptions)
}

fun NavController.authorizedAction(action: () -> Unit) {
    if (App.pinComponent.isPinSet) {
        getNavigationResult(PinModule.requestKey) { bundle ->
            val resultCode = bundle.getInt(PinModule.requestResult)

            if (resultCode == PinModule.RESULT_OK) {
                action.invoke()
            }
        }
        slideFromBottom(R.id.unlockPinFragment, bundleOf(PinModule.keyShowCancel to true))
    } else {
        action.invoke()
    }
}

fun NavController.navigateWithTermsAccepted(action: () -> Unit) {
    if (!App.termsManager.allTermsAccepted) {
        getNavigationResult(TermsFragment.resultBundleKey) { bundle ->
            val agreedToTerms = bundle.getInt(TermsFragment.requestResultKey)

            if (agreedToTerms == TermsFragment.RESULT_OK) {
                action.invoke()
            }
        }
        slideFromBottom(R.id.termsFragment)
    } else {
        action.invoke()
    }
}

fun NavController.navigateToSetPin(onSuccess: () -> Unit) {
    getNavigationResult(PinModule.requestKey) { bundle ->
        val resultCode = bundle.getInt(PinModule.requestResult)

        if (resultCode == PinModule.RESULT_OK) {
            onSuccess.invoke()
        }
    }

    slideFromRight(R.id.setPinFragment)
}
