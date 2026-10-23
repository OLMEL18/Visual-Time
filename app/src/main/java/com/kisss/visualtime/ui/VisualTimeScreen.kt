package com.kisss.visualtime.ui

import android.os.SystemClock
import android.widget.Toast
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.SubcomposeLayout
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
import kotlinx.coroutines.awaitCancellation

@Composable
fun VisualTimeScreen(
    viewModel: VisualTimeViewModel,
    modifier: Modifier = Modifier,
    onTimerStarted: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hostView = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val timerSurfaceInteraction = remember { MutableInteractionSource() }
    var finishedGraceActive by remember { mutableStateOf(false) }

    LaunchedEffect(state.phase, lifecycleOwner) {
        if (state.phase != TimerPhase.Finished) {
            finishedGraceActive = false
            return@LaunchedEffect
        }

        var accumulatedVisibleMillis = 0L
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val visibleSegmentStartedAt = SystemClock.elapsedRealtime()
            finishedGraceActive = true
            try {
                delay(
                    (ScreenAwakePolicy.FinishedGraceMillis - accumulatedVisibleMillis)
                        .coerceAtLeast(0L),
                )
                accumulatedVisibleMillis = ScreenAwakePolicy.FinishedGraceMillis
                finishedGraceActive = false
                awaitCancellation()
            } finally {
                if (accumulatedVisibleMillis < ScreenAwakePolicy.FinishedGraceMillis) {
                    accumulatedVisibleMillis = (
                        accumulatedVisibleMillis +
                            (SystemClock.elapsedRealtime() - visibleSegmentStartedAt)
                        ).coerceAtMost(ScreenAwakePolicy.FinishedGraceMillis)
                }
                finishedGraceActive = false
            }
        }
    }

    val keepScreenOn = ScreenAwakePolicy.shouldKeepScreenOn(
        phase = state.phase,
        finishedGraceActive = finishedGraceActive,
    )
    DisposableEffect(hostView, keepScreenOn) {
        hostView.keepScreenOn = keepScreenOn
        onDispose { hostView.keepScreenOn = false }
    }

    BackHandler(enabled = state.phase != TimerPhase.Setup) {
        viewModel.onSystemBack()
    }

    LaunchedEffect(viewModel) {
        viewModel.initializeRecentDurations()
    }

    LaunchedEffect(state.backgroundAlertWarning) {
        val warning = state.backgroundAlertWarning ?: return@LaunchedEffect
        val message = when (warning) {
            com.kisss.visualtime.BackgroundAlertWarning.MayBeDelayed ->
                R.string.background_alert_may_be_delayed
            com.kisss.visualtime.BackgroundAlertWarning.Unavailable ->
                R.string.background_alert_unavailable
        }
        Toast.makeText(context, context.getString(message), Toast.LENGTH_LONG).show()
        viewModel.consumeBackgroundAlertWarning()
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
        SharedHourglass(
            elapsedProgress = state.elapsedProgress,
            isFlowing = state.phase == TimerPhase.Running,
            referenceMinutes = state.quickStartMinutes,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = state.phase == TimerPhase.Setup,
            enter = fadeIn(tween(SETUP_FADE_MILLIS)),
            exit = fadeOut(tween(SETUP_FADE_MILLIS)),
            modifier = Modifier.fillMaxSize(),
        ) {
            SetupControls(
                minutes = state.quickStartMinutes,
                manualDraft = state.manualDraft,
                validationMessage = state.validationMessage,
                onQuickStart = { duration ->
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    coroutineScope.launch {
                        viewModel.startQuickDuration(duration)
                        onTimerStarted()
                    }
                },
                onManualDraftChanged = viewModel::onManualDraftChanged,
                onManualStart = {
                    coroutineScope.launch {
                        if (viewModel.startManualDuration()) {
                            onTimerStarted()
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (state.phase == TimerPhase.Finished) {
            FinishedContent(
                formattedTime = state.formattedRemainingTime.orEmpty(),
                referenceMinutes = state.quickStartMinutes,
                onRepeat = {
                    coroutineScope.launch {
                        viewModel.repeat()
                        onTimerStarted()
                    }
                },
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
                referenceMinutes = state.quickStartMinutes,
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
private fun SetupControls(
    minutes: List<Int>,
    manualDraft: String,
    validationMessage: String?,
    onQuickStart: (Int) -> Unit,
    onManualDraftChanged: (String) -> Unit,
    onManualStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        SharedTopControlHost(
            modifier = Modifier.fillMaxSize(),
        ) {
            SetupQuickStartPanel(minutes = minutes, onQuickStart = onQuickStart)
        }
        SharedBottomControlHost(
            referenceMinutes = minutes,
            modifier = Modifier.fillMaxSize(),
        ) {
            ManualDurationPanel(
                manualDraft = manualDraft,
                validationMessage = validationMessage,
                onManualDraftChanged = onManualDraftChanged,
                onManualStart = onManualStart,
            )
        }
    }
}

private enum class SharedLayoutSlot {
    Hourglass,
    TopReference,
    BottomReference,
    CurrentControls,
}

@Composable
private fun SharedHourglass(
    elapsedProgress: Float,
    isFlowing: Boolean,
    referenceMinutes: List<Int>,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val navigationBottom = WindowInsets.navigationBars.getBottom(density)
    val keyboardLift = (imeBottom - navigationBottom).coerceAtLeast(0)

    SubcomposeLayout(modifier = modifier) { constraints ->
        val panelConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val topReference = subcompose(SharedLayoutSlot.TopReference) {
            SetupQuickStartPanel(minutes = referenceMinutes, onQuickStart = {})
        }.single().measure(panelConstraints)
        val bottomReference = subcompose(SharedLayoutSlot.BottomReference) {
            ManualDurationPanel(
                manualDraft = "",
                validationMessage = null,
                onManualDraftChanged = {},
                onManualStart = {},
            )
        }.single().measure(panelConstraints)
        val placement = SetupLayoutPolicy.calculate(
            width = constraints.maxWidth,
            height = constraints.maxHeight,
            topPanelHeight = topReference.height,
            bottomPanelRestingHeight = (bottomReference.height - keyboardLift).coerceAtLeast(0),
            minimumGap = SETUP_MINIMUM_GAP_DP.dp.roundToPx(),
        )
        val hourglass = subcompose(SharedLayoutSlot.Hourglass) {
            HourglassCanvas(
                elapsedProgress = elapsedProgress,
                isFlowing = isFlowing,
                modifier = Modifier.fillMaxSize(),
            )
        }.single().measure(
            Constraints.fixed(constraints.maxWidth, placement.hourglassCanvasHeight),
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            hourglass.placeRelative(0, placement.hourglassCanvasY)
        }
    }
}

@Composable
private fun SharedTopControlHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val navigationBottom = WindowInsets.navigationBars.getBottom(density)
    val keyboardLift = (imeBottom - navigationBottom).coerceAtLeast(0)

    SubcomposeLayout(modifier = modifier) { constraints ->
        val panelConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val topPanel = subcompose(SharedLayoutSlot.CurrentControls, content)
            .single().measure(panelConstraints)
        val bottomReference = subcompose(SharedLayoutSlot.BottomReference) {
            ManualDurationPanel(
                manualDraft = "",
                validationMessage = null,
                onManualDraftChanged = {},
                onManualStart = {},
            )
        }.single().measure(panelConstraints)
        val placement = SetupLayoutPolicy.calculate(
            width = constraints.maxWidth,
            height = constraints.maxHeight,
            topPanelHeight = topPanel.height,
            bottomPanelRestingHeight = (bottomReference.height - keyboardLift).coerceAtLeast(0),
            minimumGap = SETUP_MINIMUM_GAP_DP.dp.roundToPx(),
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            topPanel.placeRelative(0, placement.topPanelY)
        }
    }
}

@Composable
private fun SharedBottomControlHost(
    referenceMinutes: List<Int>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val navigationBottom = WindowInsets.navigationBars.getBottom(density)
    val keyboardLift = (imeBottom - navigationBottom).coerceAtLeast(0)

    SubcomposeLayout(modifier = modifier) { constraints ->
        val panelConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val topReference = subcompose(SharedLayoutSlot.TopReference) {
            SetupQuickStartPanel(minutes = referenceMinutes, onQuickStart = {})
        }.single().measure(panelConstraints)
        val bottomReference = subcompose(SharedLayoutSlot.BottomReference) {
            ManualDurationPanel(
                manualDraft = "",
                validationMessage = null,
                onManualDraftChanged = {},
                onManualStart = {},
            )
        }.single().measure(panelConstraints)
        val currentControls = subcompose(SharedLayoutSlot.CurrentControls, content)
            .single().measure(panelConstraints)
        val placement = SetupLayoutPolicy.calculate(
            width = constraints.maxWidth,
            height = constraints.maxHeight,
            topPanelHeight = topReference.height,
            bottomPanelRestingHeight = (bottomReference.height - keyboardLift).coerceAtLeast(0),
            minimumGap = SETUP_MINIMUM_GAP_DP.dp.roundToPx(),
        )
        val keyboardSafeY = constraints.maxHeight - currentControls.height
        val currentY = minOf(placement.bottomPanelRestingY, keyboardSafeY)

        layout(constraints.maxWidth, constraints.maxHeight) {
            currentControls.placeRelative(0, currentY)
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
    referenceMinutes: List<Int>,
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

        SharedBottomControlHost(
            referenceMinutes = referenceMinutes,
            modifier = Modifier.fillMaxSize(),
        ) {
            FinishedControlPanel(onRepeat = onRepeat, onBack = onBack)
        }
    }
}

@Composable
private fun FinishedControlPanel(
    onRepeat: () -> Unit,
    onBack: () -> Unit,
) {
    Surface(
        color = DarkCharcoal.copy(alpha = 0.94f),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        modifier = Modifier
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

@Composable
internal fun TimerOverlay(
    formattedTime: String,
    isPaused: Boolean,
    referenceMinutes: List<Int>,
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

        SharedBottomControlHost(
            referenceMinutes = referenceMinutes,
            modifier = Modifier.fillMaxSize(),
        ) {
            TimerControlPanel(
                isPaused = isPaused,
                onPauseResume = onPauseResume,
                onRestart = onRestart,
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun TimerControlPanel(
    isPaused: Boolean,
    onPauseResume: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit,
) {
    Surface(
        color = DarkCharcoal.copy(alpha = 0.94f),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        modifier = Modifier
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
private const val SETUP_MINIMUM_GAP_DP = 12
