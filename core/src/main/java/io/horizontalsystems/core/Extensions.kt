package io.horizontalsystems.core

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.IdRes
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.createViewModelLazy
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavController
import io.horizontalsystems.core.fragment.KeptNavHostFragment
import io.horizontalsystems.core.helpers.SingleClickListener

//  View

fun View.setOnSingleClickListener(l: ((v: View) -> Unit)) {
    this.setOnClickListener(object : SingleClickListener() {
        override fun onSingleClick(v: View) {
            l.invoke(v)
        }
    })
}

fun View.hideKeyboard(context: Context) {
    val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

//  Fragment

fun Fragment.findNavController(): NavController {
    return KeptNavHostFragment.findNavController(this)
}

fun Fragment.getNavigationResult(key: String = "result"): Bundle? {
    return findNavController().currentBackStackEntry?.savedStateHandle?.remove<Bundle>(key)
}

fun Fragment.getNavigationLiveData(key: String = "result"): LiveData<Bundle>? {
    return findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData(key)
}

fun Fragment.setNavigationResult(key: String = "result", bundle: Bundle) {
    findNavController().previousBackStackEntry?.savedStateHandle?.set(key, bundle)
}

//  Dialog

fun Dialog.dismissOnBackPressed(onDismiss: () -> Unit) {
    setOnKeyListener { _, keyCode, event ->
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            onDismiss()
            true
        } else {
            false
        }
    }
}

@MainThread
inline fun <reified VM : ViewModel> Fragment.navGraphViewModels(@IdRes navGraphId: Int, noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null): Lazy<VM> {
    val backStackEntry by lazy {
        findNavController().getBackStackEntry(navGraphId)
    }
    val storeProducer: () -> ViewModelStore = {
        backStackEntry.viewModelStore
    }
    return createViewModelLazy(VM::class, storeProducer, {
        factoryProducer?.invoke() ?: backStackEntry.defaultViewModelProviderFactory
    })
}

//  String

fun String.hexToByteArray(): ByteArray {
    return ByteArray(this.length / 2) {
        this.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }
}

//  ByteArray

fun ByteArray.toHexString(): String {
    return this.joinToString(separator = "") {
        it.toInt().and(0xff).toString(16).padStart(2, '0')
    }
}

//  Intent & Parcelable Enum

fun Intent.putParcelableExtra(key: String, value: Parcelable) {
    putExtra(key, value)
}

//Animation

inline fun getValueAnimator(
        forward: Boolean = true,
        duration: Long,
        interpolator: TimeInterpolator,
        crossinline updateListener: (progress: Float) -> Unit
): ValueAnimator {
    val a =
            if (forward) ValueAnimator.ofFloat(0f, 1f)
            else ValueAnimator.ofFloat(1f, 0f)
    a.addUpdateListener { updateListener(it.animatedValue as Float) }
    a.duration = duration
    a.interpolator = interpolator
    return a
}

inline val Int.dp: Int
    get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics).toInt()

fun View.measureHeight(): Int {
    measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    return measuredHeight
}