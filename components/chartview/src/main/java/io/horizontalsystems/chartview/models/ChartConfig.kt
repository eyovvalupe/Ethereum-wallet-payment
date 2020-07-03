package io.horizontalsystems.chartview.models

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.R
import java.math.BigDecimal

class ChartConfig(private val context: Context, attrs: AttributeSet?) {

    //  colors
    var textFont = ResourcesCompat.getFont(context, R.font.noto_sans)
    var timelineTextColor = context.getColor(R.color.grey)
    var timelineTextSize = dp2px(12f)
    var timelineTextPadding = dp2px(4f)

    var gridTextColor = context.getColor(R.color.light_grey)
    var gridLineColor = context.getColor(R.color.steel_20)
    var gridDashColor = context.getColor(R.color.white_50)
    var gridLabelColor = context.getColor(R.color.grey_50)

    var gridTextSize = dp2px(12f)
    var gridTextPadding = dp2px(4f)
    var gridEdgeOffset = dp2px(5f)

    var curveColor = context.getColor(R.color.red_d)
    var curvePressedColor = context.getColor(R.color.light)
    var curveOutdatedColor = context.getColor(R.color.grey_50)
    var curveVerticalOffset = dp2px(18f)
    var curveFastColor = Color.parseColor("#1A60FF")
    var curveSlowColor = context.getColor(R.color.yellow_d)

    var cursorColor = context.getColor(R.color.light)

    var trendUpColor = context.getColor(R.color.green_d)
    var trendDownColor = context.getColor(R.color.red_d)

    var volumeColor = context.getColor(R.color.steel_20)
    var volumeWidth = dp2px(4f)
    var volumeOffset = dp2px(8f)

    var macdHistogramUpColor = Color.parseColor("#8013D670")
    var macdHistogramDownColor = Color.parseColor("#80FF4820")
    var macdLineOffset = dp2px(2f)
    var macdHistogramOffset = dp2px(4f)

    var strokeWidth = dp2px(0.5f)
    var strokeDash = dp2px(2f)
    var strokeDashWidth = dp2px(0.5f)

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.Chart)
        try {
            trendUpColor = ta.getInt(R.styleable.Chart_trendUpColor, trendUpColor)
            trendDownColor = ta.getInt(R.styleable.Chart_trendDownColor, trendDownColor)
            timelineTextColor = ta.getInt(R.styleable.Chart_timelineTextColor, timelineTextColor)
            gridTextColor = ta.getInt(R.styleable.Chart_gridTextColor, gridTextColor)
            gridLineColor = ta.getInt(R.styleable.Chart_gridColor, gridLineColor)
            gridDashColor = ta.getInt(R.styleable.Chart_gridDashColor, gridDashColor)
            curvePressedColor = ta.getInt(R.styleable.Chart_curvePressedColor, curvePressedColor)
            curveOutdatedColor = ta.getInt(R.styleable.Chart_partialChartColor, curveOutdatedColor)
            cursorColor = ta.getInt(R.styleable.Chart_cursorColor, cursorColor)
        } finally {
            ta.recycle()
        }
    }

    //  Helper methods
    fun setTrendColor(chartData: ChartData) {
        val lastPoint = chartData.items.lastOrNull() ?: return
        if (lastPoint.timestamp < chartData.endTimestamp) {
            curveColor = curveOutdatedColor
        } else if (chartData.diff() < BigDecimal.ZERO) {
            curveColor = trendDownColor
        } else {
            curveColor = trendUpColor
        }
    }

    fun measureTextWidth(text: String): Float {
        val paint = Paint()
        val width = paint.measureText(text)

        return dp2px(width)
    }

    private fun dp2px(dps: Float): Float {
        //  Get the screen's density scale
        val scale = context.resources.displayMetrics.density
        //  Convert the dps to pixels, based on density scale
        return dps * scale + 0.5f
    }
}
