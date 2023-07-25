package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import io.horizontalsystems.bankwallet.ui.compose.Colors
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.CurveAnimator2
import io.horizontalsystems.chartview.CurveAnimatorBars
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.chartview.models.ChartPoint
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.absoluteValue

class ChartHelper(private var target: ChartData, var hasVolumes: Boolean, private val colors: Colors) {

    private var minValue: Float = 0.0f
    private var maxValue: Float = 0.0f
    private var minKey: Long = 0
    private var maxKey: Long = 0

    private val mainCurve: CurveAnimator2
    private val movingAverageCurves = mutableMapOf<String, CurveAnimator2>()
    private var volumeBars: CurveAnimatorBars? = null
    private var rsiCurve: CurveAnimator2? = null
    private var macdLineCurve: CurveAnimator2? = null
    private var macdSignalCurve: CurveAnimator2? = null
    private var macdHistogramBars: CurveAnimatorBars? = null

    var mainCurveColor: Color = colors.greenD
    var mainCurveGradientColors: Pair<Color, Color> = Pair(Color(0x00416BFF), Color(0x8013D670))

    init {
        setExtremum()

        mainCurve = CurveAnimator2(
            target.valuesByTimestamp(),
            minKey,
            maxKey,
            minValue,
            maxValue
        )

        initMovingAverages()

        initRsiCurve()
        initMacdCurves()

        if (hasVolumes) {
            val volumeByTimestamp = target.volumeByTimestamp()
            val volumeMin = volumeByTimestamp.minOf { it.value }
            val volumeMax = volumeByTimestamp.maxOf { it.value }
            volumeBars = CurveAnimatorBars(
                volumeByTimestamp,
                minKey,
                maxKey,
                volumeMin,
                volumeMax
            )
        }

        defineColors()
    }

    private fun defineColors() {
        val chartData = target

        when {
            chartData.disabled -> {
                mainCurveColor = colors.grey
                mainCurveGradientColors = Pair(colors.grey50.copy(alpha = 0f), colors.grey50.copy(alpha = 0.5f))
            }

            !chartData.isMovementChart -> {
                mainCurveColor = colors.jacob
                mainCurveGradientColors = Pair(Color(0x00FFA800), Color(0x80FFA800))
            }

            chartData.diff() < BigDecimal.ZERO -> {
                mainCurveColor = colors.redD
                mainCurveGradientColors = Pair(Color(0x007413D6), Color(0x80FF0303))
            }

            else -> {
                mainCurveColor = colors.greenD
                mainCurveGradientColors = Pair(Color(0x00416BFF), Color(0x8013D670))
            }
        }
    }

    private fun initMacdCurves() {
        val macd = target.macd
        if (macd != null) {
            val macdLine = macd.macdLine
            val signalLine = macd.signalLine

            val extremum = listOf(
                macdLine.minOfOrNull { it.value },
                macdLine.maxOfOrNull { it.value },
                signalLine.minOfOrNull { it.value },
                signalLine.maxOfOrNull { it.value },
            )
            val absMax = extremum.mapNotNull { it?.absoluteValue }.maxOrNull() ?: 0f
            val absMin = absMax * -1

            macdLineCurve = CurveAnimator2(
                macdLine,
                minKey,
                maxKey,
                absMin,
                absMax
            )
            macdSignalCurve = CurveAnimator2(
                signalLine,
                minKey,
                maxKey,
                absMin,
                absMax
            )
            val histogram = macd.histogram
            val histogramAbsMax = histogram.values.maxOfOrNull { it.absoluteValue } ?: 0f
            val histogramAbsMin = histogramAbsMax * -1
            macdHistogramBars = CurveAnimatorBars(
                histogram,
                minKey,
                maxKey,
                histogramAbsMin,
                histogramAbsMax
            )
        }
    }

    private fun initRsiCurve() {
        rsiCurve = target.rsi?.let { chartIndicatorRsi ->
            val values = chartIndicatorRsi.points
            CurveAnimator2(
                values,
                minKey,
                maxKey,
                values.minOf { it.value },
                values.maxOf { it.value }
            )
        }
    }

    private fun initMovingAverages() {
        movingAverageCurves.clear()
        movingAverageCurves.putAll(
            target.movingAverages
                .map { (id, movingAverage: ChartIndicator) ->
                    id to CurveAnimator2(
                        movingAverage.line,
                        minKey,
                        maxKey,
                        minValue,
                        maxValue
                    ).apply {
                        color = movingAverage.color
                    }
                }
        )
    }

    fun getMainCurveState(): CurveAnimator2.UiState {
        return mainCurve.state
    }

    fun getVolumeBarsState(): CurveAnimatorBars.UiState? {
        return volumeBars?.state
    }

    fun getMovingAverageCurveStates(): List<CurveAnimator2.UiState> {
        return movingAverageCurves.map { it.value.state }
    }

    fun getRsiCurveState(): CurveAnimator2.UiState? {
        return rsiCurve?.state
    }

    fun getMacdLineCurveState(): CurveAnimator2.UiState? {
        return macdLineCurve?.state
    }

    fun getMacdSignalCurveState(): CurveAnimator2.UiState? {
        return macdSignalCurve?.state
    }

    fun getMacdHistogramBarsState(): CurveAnimatorBars.UiState? {
        return macdHistogramBars?.state
    }

    private fun setExtremum() {
        minValue = target.minValue
        maxValue = target.maxValue

        if (minValue == maxValue) {
            minValue *= 0.9f
        }

        minKey = target.startTimestamp
        maxKey = target.endTimestamp

        if (minKey == maxKey) {
            minKey = (minKey * 0.9).toLong()
            maxKey = (maxKey * 1.1).toLong()
        }
    }

    fun setTarget(chartData: ChartData, hasVolumes: Boolean) {
        target = chartData
        this.hasVolumes = hasVolumes
        setExtremum()

        mainCurve.setTo(
            target.valuesByTimestamp(),
            minKey,
            maxKey,
            minValue,
            maxValue
        )

        if (target.movingAverages.keys != movingAverageCurves.keys) {
            initMovingAverages()
        } else {
            target.movingAverages.forEach { (id, u: ChartIndicator) ->
                movingAverageCurves[id]?.setTo(
                    u.line,
                    minKey,
                    maxKey,
                    minValue,
                    maxValue,
                )
            }
        }

        if (target.rsi == null) {
            rsiCurve = null
        } else if (rsiCurve == null) {
            initRsiCurve()
        } else {
            target.rsi?.let { chartIndicatorRsi ->
                val values = chartIndicatorRsi.points
                rsiCurve?.setTo(
                    values,
                    minKey,
                    maxKey,
                    values.minOf { it.value },
                    values.maxOf { it.value }
                )
            }
        }

        if (target.macd == null) {
            macdLineCurve = null
            macdSignalCurve = null
            macdHistogramBars = null
        } else if (macdLineCurve == null || macdSignalCurve == null || macdHistogramBars == null) {
            initMacdCurves()
        } else {
            target.macd?.let { macd ->
                val macdLine = macd.macdLine
                val signalLine = macd.signalLine

                val extremum = listOf(
                    macdLine.minOfOrNull { it.value },
                    macdLine.maxOfOrNull { it.value },
                    signalLine.minOfOrNull { it.value },
                    signalLine.maxOfOrNull { it.value },
                )
                val absMax = extremum.mapNotNull { it?.absoluteValue }.maxOrNull() ?: 0f
                val absMin = absMax * -1

                macdLineCurve?.setTo(
                    macdLine,
                    minKey,
                    maxKey,
                    absMin,
                    absMax
                )
                macdSignalCurve?.setTo(
                    signalLine,
                    minKey,
                    maxKey,
                    absMin,
                    absMax
                )
                val histogram = macd.histogram
                val histogramAbsMax = histogram.values.maxOfOrNull { it.absoluteValue } ?: 0f
                val histogramAbsMin = histogramAbsMax * -1
                macdHistogramBars?.setValues(
                    histogram,
                    minKey,
                    maxKey,
                    histogramAbsMin,
                    histogramAbsMax
                )
            }
        }

        if (hasVolumes) {
            val volumeByTimestamp = target.volumeByTimestamp()
            val volumeMin = volumeByTimestamp.minOf { it.value }
            val volumeMax = volumeByTimestamp.maxOf { it.value }
            volumeBars?.setValues(
                volumeByTimestamp,
                minKey,
                maxKey,
                volumeMin,
                volumeMax
            )
        }

        defineColors()
    }

    fun onNextFrame(value: Float) {
        mainCurve.onNextFrame(value)
        movingAverageCurves.forEach { (_, curve) ->
            curve.onNextFrame(value)
        }
        volumeBars?.onNextFrame(value)
        rsiCurve?.onNextFrame(value)
        macdLineCurve?.onNextFrame(value)
        macdSignalCurve?.onNextFrame(value)
        macdHistogramBars?.onNextFrame(value)
    }

    var selectedItem by mutableStateOf<SelectedItem?>(null)
        private set

    fun setSelectedPercentagePositionX(percentagePositionX: Float?) {
        if (percentagePositionX == null) {
            selectedItem = null
            return
        }

        val interval = maxKey - minKey
        val timestamp = minKey + interval * percentagePositionX

        val nearestChartPoint = target.items.minByOrNull {
            abs(it.timestamp - timestamp)
        } ?: return

        val nearestPercentagePositionX = (nearestChartPoint.timestamp - minKey) / interval.toFloat()

        selectedItem = SelectedItem(
            percentagePositionX = nearestPercentagePositionX,
            chartPoint = nearestChartPoint
        )
    }

}

data class SelectedItem(val percentagePositionX: Float, val chartPoint: ChartPoint)
