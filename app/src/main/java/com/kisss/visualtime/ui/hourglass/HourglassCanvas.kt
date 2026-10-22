package com.kisss.visualtime.ui.hourglass

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.kisss.visualtime.ui.theme.FrameDark
import com.kisss.visualtime.ui.theme.FrameLight
import com.kisss.visualtime.ui.theme.GlassContour
import com.kisss.visualtime.ui.theme.GlassReflection
import com.kisss.visualtime.ui.theme.GlassTint
import com.kisss.visualtime.ui.theme.SandGold
import com.kisss.visualtime.ui.theme.SandHighlight
import com.kisss.visualtime.ui.theme.SandShadow
import kotlinx.coroutines.isActive
import kotlin.math.sin

@Composable
internal fun HourglassCanvas(
    elapsedProgress: Float,
    isFlowing: Boolean,
    modifier: Modifier = Modifier,
) {
    val progress = elapsedProgress.coerceIn(0f, 1f)
    var cosmeticPhase by remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { androidx.compose.runtime.mutableStateOf(Size.Zero) }
    val geometry = remember(canvasSize) {
        canvasSize.takeIf { it.width > 0f && it.height > 0f }?.let(HourglassGeometry::from)
    }
    val frameBrush = remember(geometry) {
        geometry?.let {
            val capLeft = it.left + it.width * 0.145f
            val capWidth = it.width * 0.71f
            Brush.horizontalGradient(
                colors = listOf(
                    FrameDark.copy(alpha = 0.90f),
                    FrameLight.copy(alpha = 0.88f),
                    FrameDark.copy(alpha = 0.90f),
                ),
                startX = capLeft,
                endX = capLeft + capWidth,
            )
        }
    }
    val supportBrush = remember(geometry) {
        geometry?.let {
            Brush.verticalGradient(
                colors = listOf(
                    FrameLight.copy(alpha = 0.86f),
                    FrameDark.copy(alpha = 0.90f),
                    FrameLight.copy(alpha = 0.86f),
                ),
                startY = it.top + it.height * 0.116f,
                endY = it.top + it.height * 0.882f,
            )
        }
    }
    val sandBrush = remember(geometry) {
        geometry?.let {
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to SandHighlight,
                    0.48f to SandGold,
                    1f to SandShadow,
                ),
                startY = it.top + it.height * 0.18f,
                endY = it.top + it.height * 0.84f,
            )
        }
    }

    LaunchedEffect(isFlowing) {
        if (!isFlowing) {
            cosmeticPhase = 0f
            return@LaunchedEffect
        }

        val startNanos = withFrameNanos { it }
        var lastUpdateNanos = startNanos
        while (isActive) {
            withFrameNanos { frameNanos ->
                if (frameNanos - lastUpdateNanos >= 33_000_000L) {
                    cosmeticPhase = (frameNanos - startNanos) / 1_000_000_000f
                    lastUpdateNanos = frameNanos
                }
            }
        }
    }

    Canvas(
        modifier = modifier.onSizeChanged {
            val newSize = Size(it.width.toFloat(), it.height.toFloat())
            if (newSize != canvasSize) canvasSize = newSize
        },
    ) {
        val geometry = geometry ?: return@Canvas
        val frameBrush = frameBrush ?: return@Canvas
        val supportBrush = supportBrush ?: return@Canvas
        val sandBrush = sandBrush ?: return@Canvas
        val contourStroke = 1.35.dp.toPx()
        val reflectionStroke = 1.05.dp.toPx()
        val supportWidth = geometry.width * 0.021f
        val capHeight = geometry.height * 0.030f
        val capLeft = geometry.left + geometry.width * 0.145f
        val capWidth = geometry.width * 0.71f

        drawRoundRect(
            brush = frameBrush,
            topLeft = Offset(capLeft, geometry.top + geometry.height * 0.095f),
            size = Size(capWidth, capHeight),
            cornerRadius = CornerRadius(capHeight * 0.45f),
        )
        drawRoundRect(
            brush = frameBrush,
            topLeft = Offset(capLeft, geometry.top + geometry.height * 0.869f),
            size = Size(capWidth, capHeight),
            cornerRadius = CornerRadius(capHeight * 0.45f),
        )
        drawRoundRect(
            brush = supportBrush,
            topLeft = Offset(
                geometry.left + geometry.width * 0.16f,
                geometry.top + geometry.height * 0.116f,
            ),
            size = Size(supportWidth, geometry.height * 0.766f),
            cornerRadius = CornerRadius(supportWidth / 2f),
        )
        drawRoundRect(
            brush = supportBrush,
            topLeft = Offset(
                geometry.left + geometry.width * 0.814f,
                geometry.top + geometry.height * 0.116f,
            ),
            size = Size(supportWidth, geometry.height * 0.766f),
            cornerRadius = CornerRadius(supportWidth / 2f),
        )

        drawPath(geometry.outerGlass, color = GlassTint)

        geometry.upperSandPath(progress)?.let { upperSand ->
            clipPath(geometry.upperChamber) {
                drawPath(upperSand, brush = sandBrush)
            }
        }

        geometry.lowerSandPath(progress)?.let { lowerSand ->
            clipPath(geometry.lowerChamber) {
                drawPath(lowerSand, brush = sandBrush)
            }
        }

        if (isFlowing && progress < 1f) {
            // The throat endpoint is invariant. A small composite drift is applied only
            // near the impact point so the stream never swings as a rigid pendulum.
            val requestedImpactOffset = geometry.width * 0.0035f * (
                0.55f * sin(cosmeticPhase * 1.37f) +
                    0.28f * sin(cosmeticPhase * 0.73f + 1.1f) +
                    0.17f * sin(cosmeticPhase * 2.11f + 2.4f)
                )
            val impactPoint = geometry.lowerImpactPoint(progress, requestedImpactOffset)
            val streamStart = Offset(geometry.centerX, geometry.streamStartY)
            val streamEnd = impactPoint.copy(
                y = impactPoint.y.coerceAtLeast(streamStart.y + geometry.height * 0.012f),
            )
            val verticalSpan = streamEnd.y - streamStart.y
            val horizontalSpan = streamEnd.x - streamStart.x
            val streamPath = Path().apply {
                moveTo(streamStart.x, streamStart.y)
                cubicTo(
                    streamStart.x,
                    streamStart.y + verticalSpan * 0.34f,
                    streamStart.x + horizontalSpan * 0.16f,
                    streamStart.y + verticalSpan * 0.72f,
                    streamEnd.x,
                    streamEnd.y,
                )
            }
            val streamWidth = 1.90.dp.toPx()

            drawPath(
                path = streamPath,
                color = SandGold,
                style = Stroke(width = streamWidth, cap = StrokeCap.Round),
            )
            drawPath(
                path = streamPath,
                color = SandHighlight.copy(alpha = 0.30f),
                style = Stroke(width = 0.42.dp.toPx(), cap = StrokeCap.Round),
            )
            drawOval(
                color = SandHighlight.copy(alpha = 0.24f),
                topLeft = Offset(
                    impactPoint.x - streamWidth * 1.35f,
                    impactPoint.y - 0.75.dp.toPx(),
                ),
                size = Size(streamWidth * 2.7f, 1.5.dp.toPx()),
            )
        }

        drawPath(
            path = geometry.outerGlass,
            color = GlassContour.copy(alpha = 0.30f),
            style = Stroke(width = contourStroke * 0.85f),
        )
        drawPath(
            path = geometry.innerGlassContour,
            color = GlassContour,
            style = Stroke(width = contourStroke),
        )
        drawPath(
            path = geometry.leftReflection,
            color = GlassReflection,
            style = Stroke(width = reflectionStroke, cap = StrokeCap.Round),
        )
        drawPath(
            path = geometry.rightReflection,
            color = GlassReflection.copy(alpha = 0.22f),
            style = Stroke(width = reflectionStroke * 0.75f, cap = StrokeCap.Round),
        )
    }
}
