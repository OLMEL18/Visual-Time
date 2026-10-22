package com.kisss.visualtime

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kisss.visualtime.ui.PrototypeHourglassScreen
import com.kisss.visualtime.ui.theme.VisualTimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        val hasStaticProgress = intent.hasExtra(EXTRA_PROTOTYPE_PROGRESS)
        val staticProgress = if (hasStaticProgress) {
            intent.getFloatExtra(EXTRA_PROTOTYPE_PROGRESS, 0f).coerceIn(0f, 1f)
        } else {
            null
        }
        val staticFlowing = hasStaticProgress &&
            intent.getBooleanExtra(EXTRA_PROTOTYPE_FLOWING, false)

        setContent {
            VisualTimeTheme {
                PrototypeHourglassScreen(
                    staticProgress = staticProgress,
                    staticFlowing = staticFlowing,
                )
            }
        }
    }

    private companion object {
        const val EXTRA_PROTOTYPE_PROGRESS = "prototype_progress"
        const val EXTRA_PROTOTYPE_FLOWING = "prototype_flowing"
    }
}
