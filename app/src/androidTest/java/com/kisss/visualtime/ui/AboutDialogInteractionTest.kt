package com.kisss.visualtime.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.kisss.visualtime.ui.theme.VisualTimeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AboutDialogInteractionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun dialogShowsVersionAndOpensWebsiteLink() {
        var openedUrl: String? = null

        composeRule.setContent {
            VisualTimeTheme {
                AboutDialog(
                    versionName = "0.1.0",
                    onDismiss = {},
                    onOpenLink = { openedUrl = it },
                )
            }
        }

        composeRule.onNodeWithText("About Visual Time").assertIsDisplayed()
        composeRule.onNodeWithText("Version: 0.1.0").assertIsDisplayed()
        composeRule.onNodeWithText("Website\nhttps://kiss.slet.org.ua/")
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals("https://kiss.slet.org.ua/", openedUrl)
        }
    }

    @Test
    fun closeActionDismissesDialog() {
        var dismissals = 0

        composeRule.setContent {
            VisualTimeTheme {
                AboutDialog(
                    versionName = "0.1.0",
                    onDismiss = { dismissals += 1 },
                    onOpenLink = {},
                )
            }
        }

        composeRule.onNodeWithText("Close").performClick()

        composeRule.runOnIdle {
            assertEquals(1, dismissals)
        }
    }
}
