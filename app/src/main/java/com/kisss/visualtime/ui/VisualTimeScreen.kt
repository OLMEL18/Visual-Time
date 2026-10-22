package com.kisss.visualtime.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kisss.visualtime.R
import com.kisss.visualtime.TimerPhase
import com.kisss.visualtime.VisualTimeViewModel
import com.kisss.visualtime.ui.hourglass.HourglassCanvas
import com.kisss.visualtime.ui.theme.DarkCharcoal
import com.kisss.visualtime.ui.theme.SandGold
import com.kisss.visualtime.ui.theme.SoftWhite
import com.kisss.visualtime.ui.theme.TrueBlack
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun VisualTimeScreen(
    viewModel: VisualTimeViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(state.phase, lifecycleOwner) {
        if (state.phase != TimerPhase.Running) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.refreshFromClock()
            while (isActive) {
                delay(TIMER_PULSE_MILLIS)
                viewModel.refreshFromClock()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TrueBlack),
    ) {
        HourglassCanvas(
            elapsedProgress = state.elapsedProgress,
            isFlowing = state.phase == TimerPhase.Running,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = state.phase == TimerPhase.Setup,
            enter = fadeIn(tween(SETUP_FADE_MILLIS)),
            exit = fadeOut(tween(SETUP_FADE_MILLIS)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            SetupQuickStarts(
                minutes = state.quickStartMinutes,
                onStart = viewModel::startQuickDuration,
            )
        }

        if (state.phase == TimerPhase.Finished) {
            FinishedContent(
                formattedTime = state.formattedRemainingTime.orEmpty(),
                onRepeat = viewModel::repeat,
                onBack = viewModel::backToSetup,
            )
        }
    }
}

@Composable
private fun SetupQuickStarts(
    minutes: List<Int>,
    onStart: (Int) -> Unit,
) {
    Surface(
        color = DarkCharcoal.copy(alpha = 0.94f),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                color = SoftWhite,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                minutes.forEach { duration ->
                    Button(
                        onClick = { onStart(duration) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SandGold.copy(alpha = 0.92f),
                            contentColor = TrueBlack,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.quick_start_minutes, duration))
                    }
                }
            }
        }
    }
}

@Composable
private fun FinishedContent(
    formattedTime: String,
    onRepeat: () -> Unit,
    onBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.finished),
                color = SoftWhite,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = formattedTime,
                color = SoftWhite.copy(alpha = 0.88f),
                style = MaterialTheme.typography.titleLarge.merge(
                    TextStyle(fontFeatureSettings = "tnum"),
                ),
            )
        }

        Surface(
            color = DarkCharcoal.copy(alpha = 0.94f),
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 0.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(14.dp),
            ) {
                Button(
                    onClick = onRepeat,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SandGold,
                        contentColor = TrueBlack,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.repeat))
                }
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkCharcoal,
                        contentColor = SoftWhite,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.back_to_setup))
                }
            }
        }
    }
}

private const val TIMER_PULSE_MILLIS = 50L
private const val SETUP_FADE_MILLIS = 220
