package com.kisss.visualtime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kisss.visualtime.ui.hourglass.HourglassCanvas
import com.kisss.visualtime.ui.theme.TrueBlack
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val PROTOTYPE_FLOW_MILLIS = 14_000L
private const val EMPTY_HOLD_MILLIS = 2_200L
private const val FULL_HOLD_MILLIS = 1_200L

@Composable
internal fun PrototypeHourglassScreen(
    staticProgress: Float?,
    staticFlowing: Boolean,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var progress by remember(staticProgress) {
        mutableFloatStateOf(staticProgress?.coerceIn(0f, 1f) ?: 0f)
    }
    var isFlowing by remember(staticProgress, staticFlowing) {
        mutableStateOf(staticProgress != null && staticFlowing)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isResumed = true
                Lifecycle.Event.ON_PAUSE -> isResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(staticProgress, staticFlowing, isResumed) {
        if (staticProgress != null) {
            progress = staticProgress.coerceIn(0f, 1f)
            isFlowing = staticFlowing && isResumed && progress < 1f
            return@LaunchedEffect
        }

        if (!isResumed) {
            isFlowing = false
            return@LaunchedEffect
        }

        while (isActive) {
            progress = 0f
            isFlowing = false
            delay(FULL_HOLD_MILLIS)

            val startNanos = withFrameNanos { it }
            isFlowing = true
            while (isActive && progress < 1f) {
                withFrameNanos { frameNanos ->
                    val elapsedMillis = (frameNanos - startNanos) / 1_000_000f
                    progress = (elapsedMillis / PROTOTYPE_FLOW_MILLIS).coerceIn(0f, 1f)
                }
            }

            progress = 1f
            isFlowing = false
            delay(EMPTY_HOLD_MILLIS)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack),
    ) {
        HourglassCanvas(
            elapsedProgress = progress,
            isFlowing = isFlowing,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
