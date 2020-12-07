package io.horizontalsystems.bankwallet.core

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Parcelable
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.ethereumkit.core.toRawHexString
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.views.SingleClickListener

//View

fun ImageView.setCoinImage(coinCode: String, coinType: CoinType? = null) {
    setImageDrawable(AppLayoutHelper.getCoinDrawable(context, coinCode, coinType))

    val greyColor = ContextCompat.getColor(context, io.horizontalsystems.views.R.color.grey)
    val tintColorStateList = ColorStateList.valueOf(greyColor)
    imageTintList = tintColorStateList
}

fun View.setOnSingleClickListener(l: ((v: View) -> Unit)) {
    this.setOnClickListener(
            object : SingleClickListener() {
                override fun onSingleClick(v: View) {
                    l.invoke(v)
                }
            })
}

fun View.fitSystemWindowsAndAdjustResize() =
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            this.fitsSystemWindows = true
            val bottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            WindowInsetsCompat
                    .Builder()
                    .setInsets(
                            WindowInsetsCompat.Type.systemBars(),
                            Insets.of(0, 0, 0, bottom)
                    )
                    .build()
                    .apply {
                        ViewCompat.onApplyWindowInsets(v, this)
                    }
        }

// String

fun String.hexToByteArray(): ByteArray {
    return ByteArray(this.length / 2) {
        this.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }
}

// ByteArray

fun ByteArray.toRawHexString(): String {
    return this.joinToString(separator = "") {
        it.toInt().and(0xff).toString(16).padStart(2, '0')
    }
}

fun ByteArray?.toHexString(): String {
    val rawHex = this?.toRawHexString() ?: return ""
    return "0x$rawHex"
}

// Intent & Parcelable Enum
fun Intent.putParcelableExtra(key: String, value: Parcelable) {
    putExtra(key, value)
}

fun LockTimeInterval?.stringResId(): Int {
    return when (this) {
        LockTimeInterval.hour -> R.string.Send_LockTime_Hour
        LockTimeInterval.month -> R.string.Send_LockTime_Month
        LockTimeInterval.halfYear -> R.string.Send_LockTime_HalfYear
        LockTimeInterval.year -> R.string.Send_LockTime_Year
        null -> R.string.Send_LockTime_Off
    }
}
