package com.kisss.visualtime.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kisss.visualtime.ui.theme.VisualTimeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TimerOverlayInteractionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun pauseControlConsumesItsTapInsteadOfTogglingTheBackground() {
        var backgroundTaps = 0
        var pauseTaps = 0

        composeRule.setContent {
            VisualTimeTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { backgroundTaps += 1 },
                ) {
                    TimerOverlay(
                        formattedTime = "25:00",
                        isPaused = false,
                        onPauseResume = { pauseTaps += 1 },
                        onRestart = {},
                        onBack = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("25:00").assertIsDisplayed()
        composeRule.onNodeWithText("Pause").performClick()

        composeRule.runOnIdle {
            assertEquals(1, pauseTaps)
            assertEquals(0, backgroundTaps)
        }
    }

    @Test
    fun pausedOverlayUsesResumeLabelAndKeepsOtherActionsVisible() {
        composeRule.setContent {
            VisualTimeTheme {
                TimerOverlay(
                    formattedTime = "01:30:00",
                    isPaused = true,
                    onPauseResume = {},
                    onRestart = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Resume").assertIsDisplayed()
        composeRule.onNodeWithText("Restart").assertIsDisplayed()
        composeRule.onNodeWithText("Back").assertIsDisplayed()
    }
}
