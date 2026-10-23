package com.kisss.visualtime.ui

import com.kisss.visualtime.ui.hourglass.HourglassLayoutMetrics
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

internal data class SetupLayoutPlacement(
    val hourglassCanvasHeight: Int,
    val hourglassCanvasY: Int,
    val topPanelY: Int,
    val bottomPanelRestingY: Int,
    val frameTop: Float,
    val frameBottom: Float,
)

internal object SetupLayoutPolicy {
    fun calculate(
        width: Int,
        height: Int,
        topPanelHeight: Int,
        bottomPanelRestingHeight: Int,
        minimumGap: Int,
    ): SetupLayoutPlacement {
        require(width > 0 && height > 0)
        require(topPanelHeight >= 0 && bottomPanelRestingHeight >= 0 && minimumGap >= 0)

        val fullFrame = frameBounds(width = width, canvasHeight = height, canvasY = 0)
        val maximumTopGap = fullFrame.first - topPanelHeight
        val maximumBottomGap = height - bottomPanelRestingHeight - fullFrame.second

        if (maximumTopGap >= minimumGap && maximumBottomGap >= minimumGap) {
            val balancedGap = min(maximumTopGap, maximumBottomGap)
            return SetupLayoutPlacement(
                hourglassCanvasHeight = height,
                hourglassCanvasY = 0,
                topPanelY = (fullFrame.first - balancedGap - topPanelHeight).roundToInt(),
                bottomPanelRestingY = (fullFrame.second + balancedGap).roundToInt(),
                frameTop = fullFrame.first,
                frameBottom = fullFrame.second,
            )
        }

        val freeTop = topPanelHeight.toFloat()
        val freeBottom = (height - bottomPanelRestingHeight).toFloat()
        val availableFrameHeight = (freeBottom - freeTop - minimumGap * 2f).coerceAtLeast(0f)
        val frameFraction = HourglassLayoutMetrics.FrameBottomFraction -
            HourglassLayoutMetrics.FrameTopFraction
        val targetGeometryHeight = availableFrameHeight / frameFraction
        val canvasHeight = ceil(
            targetGeometryHeight / HourglassLayoutMetrics.CanvasHeightFraction,
        ).toInt().coerceIn(1, height)
        val geometryHeight = HourglassLayoutMetrics.geometryHeight(
            canvasWidth = width.toFloat(),
            canvasHeight = canvasHeight.toFloat(),
        )
        val localFrameCenter = canvasHeight / 2f + geometryHeight * (
            (HourglassLayoutMetrics.FrameTopFraction +
                HourglassLayoutMetrics.FrameBottomFraction) / 2f - 0.5f
            )
        val desiredFrameCenter = (freeTop + freeBottom) / 2f
        val canvasY = (desiredFrameCenter - localFrameCenter).roundToInt()
        val frame = frameBounds(width = width, canvasHeight = canvasHeight, canvasY = canvasY)

        return SetupLayoutPlacement(
            hourglassCanvasHeight = canvasHeight,
            hourglassCanvasY = canvasY,
            topPanelY = 0,
            bottomPanelRestingY = height - bottomPanelRestingHeight,
            frameTop = frame.first,
            frameBottom = frame.second,
        )
    }

    private fun frameBounds(
        width: Int,
        canvasHeight: Int,
        canvasY: Int,
    ): Pair<Float, Float> {
        val geometryHeight = HourglassLayoutMetrics.geometryHeight(
            canvasWidth = width.toFloat(),
            canvasHeight = canvasHeight.toFloat(),
        )
        val geometryTop = canvasY + (canvasHeight - geometryHeight) / 2f
        return Pair(
            geometryTop + geometryHeight * HourglassLayoutMetrics.FrameTopFraction,
            geometryTop + geometryHeight * HourglassLayoutMetrics.FrameBottomFraction,
        )
    }
}
