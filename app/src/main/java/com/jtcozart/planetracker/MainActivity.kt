package com.jtcozart.planetracker

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jtcozart.planetracker.ui.AppRoot
import com.jtcozart.planetracker.ui.TrackerViewModel
import com.jtcozart.planetracker.ui.theme.PlaneTrackerTheme

const val EXTRA_ICAO = "icao"

class MainActivity : ComponentActivity() {

    private val viewModel: TrackerViewModel by viewModels()
    private var pendingIcao by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingIcao = intent.getStringExtra(EXTRA_ICAO)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            PlaneTrackerTheme(themeMode = settings.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(
                        viewModel,
                        requiredPermissions(),
                        pendingIcao = pendingIcao,
                        onPendingIcaoConsumed = { pendingIcao = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIcao = intent.getStringExtra(EXTRA_ICAO)
    }

    private fun requiredPermissions(): List<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
