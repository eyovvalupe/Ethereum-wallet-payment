package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_settings_dropdown.view.*
import kotlinx.android.synthetic.main.view_settings_dropdown_text.view.*

class SettingsViewDropdown @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : SettingsViewBase(context, attrs, defStyleAttr) {

    fun showDropdownValue(text: String?) {
        dropdownValue.text = text
        dropdownValue.isVisible = text != null
    }

    fun showDropdownIcon(show: Boolean) {
        dropdownIcon.isVisible = show
    }

    init {
        inflate(context, R.layout.view_settings_dropdown, this)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.SettingsViewDropdown)
        try {
            showTitle(attributes.getString(R.styleable.SettingsViewDropdown_title))
            showSubtitle(attributes.getString(R.styleable.SettingsViewDropdown_subtitle))
            showIcon(attributes.getDrawable(R.styleable.SettingsViewDropdown_icon))
            showDropdownValue(attributes.getString(R.styleable.SettingsViewDropdown_value))
            showBottomBorder(attributes.getBoolean(R.styleable.SettingsViewDropdown_bottomBorder, false))
        } finally {
            attributes.recycle()
        }
    }
}
