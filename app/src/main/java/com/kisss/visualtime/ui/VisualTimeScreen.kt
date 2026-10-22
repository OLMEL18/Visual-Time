package com.kisss.visualtime.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import kotlinx.coroutines.launch

@Composable
fun VisualTimeScreen(
    viewModel: VisualTimeViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val timerSurfaceInteraction = remember { MutableInteractionSource() }

    BackHandler(enabled = state.phase != TimerPhase.Setup) {
        viewModel.onSystemBack()
    }

    LaunchedEffect(viewModel) {
        viewModel.initializeRecentDurations()
    }

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
            .background(TrueBlack)
            .then(
                if (state.phase == TimerPhase.Running || state.phase == TimerPhase.Paused) {
                    Modifier.clickable(
                        interactionSource = timerSurfaceInteraction,
                        indication = null,
                        onClick = viewModel::onTimerSurfaceTapped,
                    )
                } else {
                    Modifier
                },
            ),
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
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                SetupQuickStartPanel(
                    minutes = state.quickStartMinutes,
                    onQuickStart = { duration ->
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        coroutineScope.launch { viewModel.startQuickDuration(duration) }
                    },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                ManualDurationPanel(
                    manualDraft = state.manualDraft,
                    validationMessage = state.validationMessage,
                    onManualDraftChanged = viewModel::onManualDraftChanged,
                    onManualStart = {
                        coroutineScope.launch {
                            if (viewModel.startManualDuration()) {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        if (state.phase == TimerPhase.Finished) {
            FinishedContent(
                formattedTime = state.formattedRemainingTime.orEmpty(),
                onRepeat = { coroutineScope.launch { viewModel.repeat() } },
                onBack = viewModel::backToSetup,
            )
        }


        AnimatedVisibility(
            visible = state.phase in setOf(TimerPhase.Running, TimerPhase.Paused) &&
                state.overlayVisible,
            enter = fadeIn(tween(OVERLAY_FADE_MILLIS)),
            exit = fadeOut(tween(OVERLAY_FADE_MILLIS)),
        ) {
            TimerOverlay(
                formattedTime = state.formattedRemainingTime.orEmpty(),
                isPaused = state.phase == TimerPhase.Paused,
                onPauseResume = {
                    if (state.phase == TimerPhase.Paused) viewModel.resume() else viewModel.pause()
                },
                onRestart = viewModel::restart,
                onBack = viewModel::backToSetup,
            )
        }

        if (state.phase == TimerPhase.Paused && !state.overlayVisible) {
            PauseIndicator(modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun SetupQuickStartPanel(
    minutes: List<Int>,
    onQuickStart: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = DarkCharcoal.copy(alpha = 0.94f),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        modifier = modifier
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
            if (minutes.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    minutes.forEach { duration ->
                        Button(
                            onClick = { onQuickStart(duration) },
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
}

@Composable
private fun ManualDurationPanel(
    manualDraft: String,
    validationMessage: String?,
    onManualDraftChanged: (String) -> Unit,
    onManualStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = DarkCharcoal.copy(alpha = 0.94f),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        modifier = modifier
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(14.dp),
        ) {
            OutlinedTextField(
                value = manualDraft,
                onValueChange = onManualDraftChanged,
                label = { Text(stringResource(R.string.minutes_label)) },
                placeholder = { Text(stringResource(R.string.minutes_hint)) },
                singleLine = true,
                isError = validationMessage != null,
                supportingText = validationMessage?.let { message ->
                    { Text(message) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onManualStart() }),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onManualStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SandGold,
                    contentColor = TrueBlack,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.start))
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

@Composable
internal fun TimerOverlay(
    formattedTime: String,
    isPaused: Boolean,
    onPauseResume: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = DarkCharcoal.copy(alpha = 0.86f),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 0.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 18.dp),
        ) {
            Text(
                text = formattedTime,
                color = SoftWhite,
                style = MaterialTheme.typography.headlineMedium.merge(
                    TextStyle(fontFeatureSettings = "tnum"),
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .widthIn(min = 180.dp)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(14.dp),
            ) {
                OverlayButton(
                    text = stringResource(if (isPaused) R.string.resume else R.string.pause),
                    emphasized = true,
                    onClick = onPauseResume,
                    modifier = Modifier.weight(1f),
                )
                OverlayButton(
                    text = stringResource(R.string.restart),
                    onClick = onRestart,
                    modifier = Modifier.weight(1f),
                )
                OverlayButton(
                    text = stringResource(R.string.back),
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun OverlayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (emphasized) SandGold else DarkCharcoal,
            contentColor = if (emphasized) TrueBlack else SoftWhite,
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Text(text)
    }
}

@Composable
private fun PauseIndicator(modifier: Modifier = Modifier) {
    Surface(
        color = DarkCharcoal.copy(alpha = 0.72f),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp,
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 18.dp)
            .widthIn(min = 86.dp),
    ) {
        Text(
            text = stringResource(R.string.paused),
            color = SoftWhite.copy(alpha = 0.82f),
            style = MaterialTheme.typography.labelLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

private const val TIMER_PULSE_MILLIS = 50L
private const val SETUP_FADE_MILLIS = 220
private const val OVERLAY_FADE_MILLIS = 160
