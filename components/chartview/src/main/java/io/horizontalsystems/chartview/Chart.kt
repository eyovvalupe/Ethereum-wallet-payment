package io.horizontalsystems.chartview

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.chartview.databinding.ViewChartBinding
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.helpers.PointConverter
import io.horizontalsystems.chartview.models.ChartConfig
import io.horizontalsystems.chartview.models.ChartIndicatorType
import io.horizontalsystems.chartview.models.ChartPoint

class Chart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    lateinit var chartViewType: ChartViewType

    private val binding = ViewChartBinding.inflate(LayoutInflater.from(context), this)
    private val indicatorAnimatedCurves = mutableMapOf<ChartIndicatorType, AnimatedCurve>()

    interface Listener {
        fun onTouchDown()
        fun onTouchUp()
        fun onTouchSelect(item: ChartPoint, indicators: Map<ChartIndicatorType, Float>)
    }

    private val config = ChartConfig(context, attrs)
    private val animatorMain = ChartAnimator {
        binding.chartMain.invalidate()
    }
    private val animatorBottom = ChartAnimator { binding.chartBottom.invalidate() }
    private val animatorTopBottomRange = ChartAnimator { binding.topLowRange.invalidate() }

    private val mainBars = ChartBars(
        animatorMain,
        config.barColor,
        config.volumeMinHeight,
        config.volumeWidth,
        config.horizontalOffset,
    )
    private val mainCurve = ChartCurve2(config)
    private val mainGradient = ChartGradient(animatorMain)

    private val mainRange = ChartGridRange(config)

    private val bottomVolume = ChartBars(
        animatorBottom,
        config.volumeColor,
        config.volumeMinHeight,
        config.volumeWidth,
        config.horizontalOffset,
    )

    private var mainCurveAnimator: CurveAnimator? = null
    private var dominanceCurveAnimator: CurveAnimator? = null

    init {
        animatorMain.addUpdateListener {
            mainCurveAnimator?.nextFrame(animatorMain.animatedFraction)
            dominanceCurveAnimator?.nextFrame(animatorMain.animatedFraction)

            indicatorAnimatedCurves.forEach { (_, animatedCurve) ->
                animatedCurve.animator.nextFrame(animatorMain.animatedFraction)
            }
        }
    }

    fun setIndicatorLineVisible(v: Boolean) {
        binding.chartBottom.isVisible = v
    }

    fun setListener(listener: Listener) {
        binding.chartTouch.onUpdate(object : Listener {
            override fun onTouchDown() {
                mainBars.barColor = config.barPressedColor
                mainCurve.setColor(config.curvePressedColor)
                mainGradient.setShader(config.pressedGradient)
                binding.chartMain.invalidate()
                listener.onTouchDown()
            }

            override fun onTouchUp() {
                mainBars.barColor = config.barColor
                mainCurve.setColor(config.curveColor)
                mainGradient.setShader(config.curveGradient)
                binding.chartMain.invalidate()
                listener.onTouchUp()
            }

            override fun onTouchSelect(
                item: ChartPoint,
                indicators: Map<ChartIndicatorType, Float>
            ) {
                listener.onTouchSelect(item, indicators)
            }
        })
    }

    fun showSpinner() {
        binding.root.alpha = 0.5f
        binding.chartViewSpinner.isVisible = true
    }

    fun hideSpinner() {
        binding.root.alpha = 1f
        binding.chartViewSpinner.isVisible = false
    }

    fun setData(
        data: ChartData,
        maxValue: String?,
        minValue: String?
    ) {
        when (chartViewType) {
            ChartViewType.Line -> {
                setDataLine(data, maxValue, minValue)
            }
            ChartViewType.Bar -> {
                setDataBars(data, maxValue, minValue)
            }
        }
    }

    private fun setDataLine(
        data: ChartData,
        maxValue: String?,
        minValue: String?
    ) {

        animatorMain.cancel()

        val candleValues = data.valuesByTimestamp()
        val minCandleValue = data.minValue
        val maxCandleValue = data.maxValue

        mainCurveAnimator = CurveAnimator(
            candleValues,
            data.startTimestamp,
            data.endTimestamp,
            minCandleValue,
            maxCandleValue,
            mainCurveAnimator,
            binding.chartMain.shape.right,
            binding.chartMain.shape.bottom,
            0f,
            0f,
            config.horizontalOffset
        )

        config.setTrendColor(data)

        val coordinates =
            PointConverter.coordinates(data, binding.chartMain.shape, 0f, config.horizontalOffset)

        //Dominance
        val dominanceCurve = ChartCurve2(config)
        val dominanceValues = data.dominanceByTimestamp()
        if (dominanceValues.isNotEmpty()) {
            dominanceCurveAnimator = CurveAnimator(
                dominanceValues,
                data.startTimestamp,
                data.endTimestamp,
                dominanceValues.values.minOrNull() ?: 0f,
                dominanceValues.values.maxOrNull() ?: 0f,
                dominanceCurveAnimator,
                binding.chartMain.shape.right,
                binding.chartMain.shape.bottom,
                0f,
                0f,
                config.horizontalOffset,
            )

            dominanceCurve.setShape(binding.chartMain.shape)
            dominanceCurve.setCurveAnimator(dominanceCurveAnimator!!)
            dominanceCurve.setColor(config.curveSlowColor)
        }

        val indicators = data.indicators
        val tmpIndicatorAnimatedCurves = LinkedHashMap(indicatorAnimatedCurves)
        indicatorAnimatedCurves.clear()
        indicators.forEach { (chartIndicatorType, values) ->
            if (chartIndicatorType is ChartIndicatorType.MovingAverage) {
                val indicatorAnimatedCurve = tmpIndicatorAnimatedCurves.remove(chartIndicatorType)

                val animator = CurveAnimator(
                    values,
                    data.startTimestamp,
                    data.endTimestamp,
                    minCandleValue,
                    maxCandleValue,
                    indicatorAnimatedCurve?.animator,
                    binding.chartMain.shape.right,
                    binding.chartMain.shape.bottom,
                    0f,
                    0f,
                    config.horizontalOffset,
                )

                val curve = ChartCurve2(config)
                curve.setShape(binding.chartMain.shape)
                curve.setCurveAnimator(animator)
                curve.setColor(Color.parseColor(chartIndicatorType.color))

                indicatorAnimatedCurves[chartIndicatorType] = AnimatedCurve(animator, curve)
            }
        }

        binding.chartTouch.configure(config, 0f)
        binding.chartTouch.setCoordinates(coordinates)

        // Candles
        mainCurve.setShape(binding.chartMain.shape)
        mainCurve.setCurveAnimator(mainCurveAnimator!!)
        mainCurve.setColor(config.curveColor)

        mainGradient.setCurveAnimator(mainCurveAnimator!!)
        mainGradient.setShape(binding.chartMain.shape)
        mainGradient.setShader(config.curveGradient)

        mainRange.setShape(binding.topLowRange.shape)
        mainRange.setValues(maxValue, minValue)

        // Volume
        bottomVolume.setValues(data.volumeByTimestamp(), data.startTimestamp, data.endTimestamp)
        bottomVolume.setShape(binding.chartBottom.shape)

        // ---------------------------
        // *********
        // ---------------------------

        binding.chartMain.clear()
        binding.chartMain.add(mainCurve, mainGradient)
        binding.chartMain.add(dominanceCurve)

        indicatorAnimatedCurves.forEach { (_, animatedCurve) ->
            binding.chartMain.add(animatedCurve.curve)
        }

        binding.topLowRange.clear()
        binding.topLowRange.add(mainRange)

        binding.chartBottom.clear()
        binding.chartBottom.add(bottomVolume)

        animatorMain.start()
        animatorTopBottomRange.start()
        animatorBottom.start()
    }

    private fun setDataBars(
        data: ChartData,
        maxValue: String?,
        minValue: String?
    ) {
        animatorMain.cancel()

        config.setTrendColor(data)

        val coordinates =
            PointConverter.coordinates(data, binding.chartMain.shape, 0f, config.horizontalOffset)

        binding.chartTouch.configure(config, 0f)
        binding.chartTouch.setCoordinates(coordinates)

        // Candles
        mainBars.setShape(binding.chartMain.shape)
        mainBars.setValues(
            data.valuesByTimestamp(),
            data.startTimestamp,
            data.endTimestamp,
            data.minValue,
            data.maxValue,
        )

        mainRange.setShape(binding.topLowRange.shape)
        mainRange.setValues(maxValue, minValue)

        // Volume
        bottomVolume.setValues(data.volumeByTimestamp(), data.startTimestamp, data.endTimestamp)
        bottomVolume.setShape(binding.chartBottom.shape)

        // ---------------------------
        // *********
        // ---------------------------

        binding.chartMain.clear()
        binding.chartMain.add(mainBars)

        binding.topLowRange.clear()
        binding.topLowRange.add(mainRange)

        binding.chartBottom.clear()
        binding.chartBottom.add(bottomVolume)

        animatorMain.start()
        animatorTopBottomRange.start()
        animatorBottom.start()
    }

}
