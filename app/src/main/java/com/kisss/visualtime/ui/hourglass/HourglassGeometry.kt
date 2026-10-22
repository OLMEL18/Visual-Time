package com.kisss.visualtime.ui.hourglass

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

/**
 * All physical hourglass geometry comes from [physicalHalfWidthAt].
 * Glass tint/reflections may sit outside that boundary, but sand never uses a second profile.
 */
internal class HourglassGeometry private constructor(
    val bounds: Rect,
    val outerGlass: Path,
    val innerGlassContour: Path,
    val upperChamber: Path,
    val lowerChamber: Path,
    val leftReflection: Path,
    val rightReflection: Path,
) {
    val centerX: Float = bounds.center.x
    val top: Float = bounds.top
    val left: Float = bounds.left
    val width: Float = bounds.width
    val height: Float = bounds.height
    val neckY: Float = top + height * 0.5f
    val streamStartY: Float = neckY

    private val chamberExtent = height * CHAMBER_EXTENT_FRACTION
    private val upperTopY = neckY - chamberExtent
    val lowerFloorY: Float = neckY + chamberExtent
    private val finalLowerPeakY = top + height * FINAL_LOWER_PEAK_FRACTION

    private val finalLowerArea = polygonArea(buildLowerSandPoints(finalLowerPeakY))
    private val upperCapacityArea = polygonArea(buildUpperSandPoints(upperTopY, 1f))

    // One transferred amount is authoritative for both chambers. Each lookup below
    // inverts the exact polygon geometry used to draw that chamber.
    private val totalSandArea = finalLowerArea.coerceAtMost(upperCapacityArea * 0.94f)
    private val upperSurfaceLookup = buildUpperSurfaceLookup()
    private val lowerPeakLookup = buildLowerPeakLookup()

    fun upperSandPath(elapsedProgress: Float): Path? {
        val remaining = 1f - elapsedProgress.coerceIn(0f, 1f)
        if (remaining <= 0f) return null
        return buildUpperSandPoints(
            surfaceY = lookup(upperSurfaceLookup, remaining),
            remainingFraction = remaining,
        ).toClosedPath()
    }

    fun lowerSandPath(elapsedProgress: Float): Path? {
        val progress = elapsedProgress.coerceIn(0f, 1f)
        if (progress <= 0f) return null
        return buildLowerSandPoints(lookup(lowerPeakLookup, progress)).toClosedPath()
    }

    /** Returns the actual mound surface beneath a requested cosmetic horizontal offset. */
    fun lowerImpactPoint(elapsedProgress: Float, requestedOffsetX: Float = 0f): Offset {
        val progress = elapsedProgress.coerceIn(0f, 1f)
        val peakY = lookup(lowerPeakLookup, progress)
        if (peakY >= lowerFloorY) return Offset(centerX, lowerFloorY)

        val baseHalfWidth = lowerPileHalfWidthAt(lowerFloorY, peakY)
        val maximumWander = min(width * MAXIMUM_STREAM_WANDER_FRACTION, baseHalfWidth * 0.18f)
        val offsetX = requestedOffsetX.coerceIn(-maximumWander, maximumWander)
        val targetHalfWidth = abs(offsetX)
        if (targetHalfWidth < 0.01f) return Offset(centerX, peakY)

        var aboveSurfaceY = peakY
        var belowSurfaceY = lowerFloorY
        repeat(AREA_SEARCH_STEPS) {
            val candidateY = (aboveSurfaceY + belowSurfaceY) * 0.5f
            if (lowerPileHalfWidthAt(candidateY, peakY) >= targetHalfWidth) {
                belowSurfaceY = candidateY
            } else {
                aboveSurfaceY = candidateY
            }
        }
        return Offset(centerX + offsetX, belowSurfaceY)
    }

    private fun physicalHalfWidthAt(y: Float): Float {
        val distanceFraction = (abs(y - neckY) / chamberExtent).coerceIn(0f, 1f)
        return profileHalfWidth(width, distanceFraction)
    }

    private fun buildUpperSandPoints(
        surfaceY: Float,
        remainingFraction: Float,
    ): List<Offset> {
        val edgeY = surfaceY.coerceIn(upperTopY, neckY)
        val availableDepth = (neckY - edgeY).coerceAtLeast(0f)
        if (availableDepth <= 0f) return listOf(Offset(centerX, neckY))

        val edgeHalfWidth = physicalHalfWidthAt(edgeY)
        val drainBlend = smoothStep(((0.22f - remainingFraction) / 0.22f).coerceIn(0f, 1f))
        val depression = min(
            height * lerp(0.012f, 0.020f, drainBlend),
            availableDepth * lerp(0.34f, 0.58f, drainBlend),
        )
        val points = ArrayList<Offset>(UPPER_SURFACE_STEPS + WALL_PATH_STEPS * 2 + 3)

        // The free surface is symmetric and deterministic. Its two ends lie exactly
        // on the same physical wall profile used by the glass and chamber clip.
        for (step in 0..UPPER_SURFACE_STEPS) {
            val fraction = step.toFloat() / UPPER_SURFACE_STEPS
            val x = lerp(-edgeHalfWidth, edgeHalfWidth, fraction)
            val normalizedX = (abs(x) / edgeHalfWidth.coerceAtLeast(0.001f)).coerceIn(0f, 1f)
            val bowl = 1f - normalizedX.toDouble().pow(UPPER_DEPRESSION_POWER).toFloat()
            points += Offset(centerX + x, edgeY + depression * bowl)
        }

        // Everything below the free surface fills the narrowing chamber to its real
        // inner walls, including the throat; no independent inset or outlet cap exists.
        for (step in 1..WALL_PATH_STEPS) {
            val y = lerp(edgeY, neckY, step.toFloat() / WALL_PATH_STEPS)
            points += Offset(centerX + physicalHalfWidthAt(y), y)
        }
        for (step in WALL_PATH_STEPS downTo 0) {
            val y = lerp(edgeY, neckY, step.toFloat() / WALL_PATH_STEPS)
            points += Offset(centerX - physicalHalfWidthAt(y), y)
        }
        return points
    }

    private fun buildLowerSandPoints(peakY: Float): List<Offset> {
        val boundedPeakY = peakY.coerceIn(finalLowerPeakY, lowerFloorY)
        if (boundedPeakY >= lowerFloorY) return listOf(Offset(centerX, lowerFloorY))
        val points = ArrayList<Offset>(PILE_PATH_STEPS * 2 + 3)
        points += Offset(centerX, boundedPeakY)
        for (step in 1..PILE_PATH_STEPS) {
            val y = lerp(boundedPeakY, lowerFloorY, step.toFloat() / PILE_PATH_STEPS)
            points += Offset(centerX + lowerPileHalfWidthAt(y, boundedPeakY), y)
        }
        for (step in PILE_PATH_STEPS downTo 0) {
            val y = lerp(boundedPeakY, lowerFloorY, step.toFloat() / PILE_PATH_STEPS)
            points += Offset(centerX - lowerPileHalfWidthAt(y, boundedPeakY), y)
        }
        return points
    }

    private fun lowerPileHalfWidthAt(y: Float, peakY: Float): Float {
        if (peakY >= lowerFloorY || y <= peakY) return 0f
        val depth = y - peakY
        val normalizedDepth = (depth / (lowerFloorY - peakY)).coerceIn(0f, 1f)
        val curvedReposeRun = LOWER_REPOSE_RUN * lerp(0.88f, 1f, normalizedDepth)
        val freeSurfaceHalfWidth = depth * curvedReposeRun
        val wallHalfWidth = physicalHalfWidthAt(y)
        if (wallHalfWidth <= 0f) return 0f

        val widthRatio = freeSurfaceHalfWidth / wallHalfWidth
        if (widthRatio <= LOWER_WALL_CONTACT_BLEND_START) return freeSurfaceHalfWidth
        val contactBlend = smoothStep(
            ((widthRatio - LOWER_WALL_CONTACT_BLEND_START) /
                (1f - LOWER_WALL_CONTACT_BLEND_START)).coerceIn(0f, 1f),
        )
        return lerp(freeSurfaceHalfWidth.coerceAtMost(wallHalfWidth), wallHalfWidth, contactBlend)
    }

    private fun buildUpperSurfaceLookup(): FloatArray =
        FloatArray(LOOKUP_STEPS + 1) { index ->
            val remaining = index.toFloat() / LOOKUP_STEPS
            if (remaining <= 0f) {
                neckY
            } else {
                val targetArea = totalSandArea * remaining
                var highAreaY = upperTopY
                var lowAreaY = neckY
                repeat(AREA_SEARCH_STEPS) {
                    val candidateY = (highAreaY + lowAreaY) * 0.5f
                    val candidateArea = polygonArea(buildUpperSandPoints(candidateY, remaining))
                    if (candidateArea > targetArea) {
                        highAreaY = candidateY
                    } else {
                        lowAreaY = candidateY
                    }
                }
                (highAreaY + lowAreaY) * 0.5f
            }
        }

    private fun buildLowerPeakLookup(): FloatArray =
        FloatArray(LOOKUP_STEPS + 1) { index ->
            val transferred = index.toFloat() / LOOKUP_STEPS
            if (transferred <= 0f) {
                lowerFloorY
            } else {
                val targetArea = totalSandArea * transferred
                var highAreaY = finalLowerPeakY
                var lowAreaY = lowerFloorY
                repeat(AREA_SEARCH_STEPS) {
                    val candidateY = (highAreaY + lowAreaY) * 0.5f
                    val candidateArea = polygonArea(buildLowerSandPoints(candidateY))
                    if (candidateArea > targetArea) {
                        highAreaY = candidateY
                    } else {
                        lowAreaY = candidateY
                    }
                }
                (highAreaY + lowAreaY) * 0.5f
            }
        }

    private fun polygonArea(points: List<Offset>): Float {
        if (points.size < 3) return 0f
        var doubledArea = 0.0
        points.indices.forEach { index ->
            val current = points[index]
            val next = points[(index + 1) % points.size]
            doubledArea += current.x.toDouble() * next.y - next.x.toDouble() * current.y
        }
        return (abs(doubledArea) * 0.5).toFloat()
    }

    private fun List<Offset>.toClosedPath(): Path = Path().also { path ->
        if (isEmpty()) return@also
        path.moveTo(first().x, first().y)
        drop(1).forEach { point -> path.lineTo(point.x, point.y) }
        path.close()
    }

    private fun lookup(values: FloatArray, fraction: Float): Float {
        val position = fraction.coerceIn(0f, 1f) * LOOKUP_STEPS
        val lowerIndex = position.toInt().coerceAtMost(LOOKUP_STEPS)
        val upperIndex = (lowerIndex + 1).coerceAtMost(LOOKUP_STEPS)
        return lerp(values[lowerIndex], values[upperIndex], position - lowerIndex)
    }

    private fun smoothStep(value: Float): Float = value * value * (3f - 2f * value)

    companion object {
        private const val LOOKUP_STEPS = 128
        private const val AREA_SEARCH_STEPS = 20
        private const val WALL_PATH_STEPS = 36
        private const val PILE_PATH_STEPS = 64
        private const val UPPER_SURFACE_STEPS = 32
        private const val PROFILE_PATH_STEPS = 64
        private const val CHAMBER_EXTENT_FRACTION = 0.333f
        private const val FINAL_LOWER_PEAK_FRACTION = 0.625f
        private const val PROFILE_CURVE_POWER = 1.20
        private const val UPPER_DEPRESSION_POWER = 1.70
        private const val WIDE_HALF_WIDTH_FRACTION = 0.242f
        private const val THROAT_HALF_WIDTH_FRACTION = 0.0085f
        private const val GLASS_THICKNESS_FRACTION = 0.014f
        private const val LOWER_REPOSE_RUN = 0.70f
        private const val LOWER_WALL_CONTACT_BLEND_START = 0.80f
        private const val MAXIMUM_STREAM_WANDER_FRACTION = 0.004f

        fun from(canvasSize: Size): HourglassGeometry {
            val hourglassHeight = min(canvasSize.height * 0.68f, canvasSize.width * 1.42f)
            val hourglassWidth = min(canvasSize.width * 0.76f, hourglassHeight * 0.68f)
            val left = (canvasSize.width - hourglassWidth) / 2f
            val top = (canvasSize.height - hourglassHeight) / 2f
            val centerX = left + hourglassWidth * 0.5f
            val neckY = top + hourglassHeight * 0.5f
            val extent = hourglassHeight * CHAMBER_EXTENT_FRACTION
            val upperTopY = neckY - extent
            val lowerFloorY = neckY + extent

            val innerGlassContour = buildFullProfilePath(
                centerX = centerX,
                neckY = neckY,
                extent = extent,
                width = hourglassWidth,
                extraHalfWidth = 0f,
            )
            val outerGlass = buildFullProfilePath(
                centerX = centerX,
                neckY = neckY,
                extent = extent,
                width = hourglassWidth,
                extraHalfWidth = hourglassWidth * GLASS_THICKNESS_FRACTION,
            )
            val upperChamber = buildHalfProfilePath(
                centerX = centerX,
                wideY = upperTopY,
                neckY = neckY,
                extent = extent,
                width = hourglassWidth,
            )
            val lowerChamber = buildHalfProfilePath(
                centerX = centerX,
                wideY = lowerFloorY,
                neckY = neckY,
                extent = extent,
                width = hourglassWidth,
            )
            val leftReflection = buildReflectionPath(
                centerX = centerX,
                neckY = neckY,
                extent = extent,
                width = hourglassWidth,
                side = -1f,
            )
            val rightReflection = buildReflectionPath(
                centerX = centerX,
                neckY = neckY,
                extent = extent,
                width = hourglassWidth,
                side = 1f,
            )

            return HourglassGeometry(
                bounds = Rect(Offset(left, top), Size(hourglassWidth, hourglassHeight)),
                outerGlass = outerGlass,
                innerGlassContour = innerGlassContour,
                upperChamber = upperChamber,
                lowerChamber = lowerChamber,
                leftReflection = leftReflection,
                rightReflection = rightReflection,
            )
        }

        private fun buildFullProfilePath(
            centerX: Float,
            neckY: Float,
            extent: Float,
            width: Float,
            extraHalfWidth: Float,
        ): Path = Path().apply {
            val upperTopY = neckY - extent
            val lowerFloorY = neckY + extent
            val topHalfWidth = profileHalfWidth(width, 1f) + extraHalfWidth
            moveTo(centerX - topHalfWidth, upperTopY)
            lineTo(centerX + topHalfWidth, upperTopY)
            for (step in 1..PROFILE_PATH_STEPS * 2) {
                val y = lerp(upperTopY, lowerFloorY, step.toFloat() / (PROFILE_PATH_STEPS * 2))
                val distance = (abs(y - neckY) / extent).coerceIn(0f, 1f)
                lineTo(centerX + profileHalfWidth(width, distance) + extraHalfWidth, y)
            }
            lineTo(centerX - topHalfWidth, lowerFloorY)
            for (step in PROFILE_PATH_STEPS * 2 downTo 0) {
                val y = lerp(upperTopY, lowerFloorY, step.toFloat() / (PROFILE_PATH_STEPS * 2))
                val distance = (abs(y - neckY) / extent).coerceIn(0f, 1f)
                lineTo(centerX - profileHalfWidth(width, distance) - extraHalfWidth, y)
            }
            close()
        }

        private fun buildHalfProfilePath(
            centerX: Float,
            wideY: Float,
            neckY: Float,
            extent: Float,
            width: Float,
        ): Path = Path().apply {
            val wideHalfWidth = profileHalfWidth(width, 1f)
            moveTo(centerX - wideHalfWidth, wideY)
            lineTo(centerX + wideHalfWidth, wideY)
            for (step in 1..PROFILE_PATH_STEPS) {
                val y = lerp(wideY, neckY, step.toFloat() / PROFILE_PATH_STEPS)
                val distance = (abs(y - neckY) / extent).coerceIn(0f, 1f)
                lineTo(centerX + profileHalfWidth(width, distance), y)
            }
            for (step in PROFILE_PATH_STEPS downTo 0) {
                val y = lerp(wideY, neckY, step.toFloat() / PROFILE_PATH_STEPS)
                val distance = (abs(y - neckY) / extent).coerceIn(0f, 1f)
                lineTo(centerX - profileHalfWidth(width, distance), y)
            }
            close()
        }

        private fun buildReflectionPath(
            centerX: Float,
            neckY: Float,
            extent: Float,
            width: Float,
            side: Float,
        ): Path = Path().apply {
            val inset = width * if (side < 0f) 0.026f else 0.020f
            fun reflectionX(y: Float): Float {
                val distance = (abs(y - neckY) / extent).coerceIn(0f, 1f)
                return centerX + side * (profileHalfWidth(width, distance) - inset)
            }
            val upperStartY = neckY - extent * if (side < 0f) 0.86f else 0.72f
            val upperEndY = neckY - extent * if (side < 0f) 0.13f else 0.26f
            moveTo(reflectionX(upperStartY), upperStartY)
            for (step in 1..18) {
                val y = lerp(upperStartY, upperEndY, step / 18f)
                lineTo(reflectionX(y), y)
            }
            if (side < 0f) {
                val lowerStartY = neckY + extent * 0.13f
                val lowerEndY = neckY + extent * 0.86f
                moveTo(reflectionX(lowerStartY), lowerStartY)
                for (step in 1..18) {
                    val y = lerp(lowerStartY, lowerEndY, step / 18f)
                    lineTo(reflectionX(y), y)
                }
            }
        }

        private fun profileHalfWidth(width: Float, distanceFromThroat: Float): Float =
            lerp(
                width * THROAT_HALF_WIDTH_FRACTION,
                width * WIDE_HALF_WIDTH_FRACTION,
                distanceFromThroat.coerceIn(0f, 1f).toDouble().pow(PROFILE_CURVE_POWER).toFloat(),
            )

        private fun lerp(start: Float, end: Float, fraction: Float): Float =
            start + (end - start) * fraction.coerceIn(0f, 1f)
    }
}
