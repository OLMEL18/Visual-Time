package com.kisss.visualtime

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kisss.visualtime.data.RecentDurationsRepository
import com.kisss.visualtime.data.visualTimeDataStore
import com.kisss.visualtime.timer.AndroidMonotonicClock
import com.kisss.visualtime.ui.VisualTimeScreen
import com.kisss.visualtime.ui.theme.VisualTimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        val recentDurationsRepository = RecentDurationsRepository(applicationContext.visualTimeDataStore)

        setContent {
            VisualTimeTheme {
                val viewModel: VisualTimeViewModel = viewModel(
                    factory = VisualTimeViewModelFactory(
                        clock = AndroidMonotonicClock,
                        recentDurationsStore = recentDurationsRepository,
                    ),
                )
                VisualTimeScreen(viewModel = viewModel)
            }
        }
    }
}
