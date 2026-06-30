package com.jtcozart.planetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtcozart.planetracker.data.Settings
import com.jtcozart.planetracker.ui.TrackerViewModel

@Composable
fun SettingsScreen(settings: Settings, viewModel: TrackerViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle("Detection")

        Text("Search radius: ${settings.radiusNm.toInt()} NM")
        Slider(
            value = settings.radiusNm,
            onValueChange = { v -> viewModel.updateSettings { it.copy(radiusNm = v) } },
            valueRange = 1f..50f,
        )

        Text("Scan interval: ${settings.pollIntervalSec} s")
        Slider(
            value = settings.pollIntervalSec.toFloat(),
            onValueChange = { v -> viewModel.updateSettings { it.copy(pollIntervalSec = v.toInt()) } },
            valueRange = Settings.MIN_POLL_INTERVAL_SEC.toFloat()..120f,
        )

        OutlinedTextField(
            value = settings.poiTypes,
            onValueChange = { v -> viewModel.updateSettings { it.copy(poiTypes = v) } },
            label = { Text("POI aircraft types (e.g. B737,F16,C172)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        ToggleRow("Enable POI filter (show only these types)", settings.poiEnabled) { on ->
            viewModel.updateSettings { it.copy(poiEnabled = on) }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SectionTitle("Notifications")

        ToggleRow("Enable notifications", settings.notificationsEnabled) { on ->
            viewModel.updateSettings { it.copy(notificationsEnabled = on) }
        }
        ToggleRow("Military", settings.notifyMilitary, settings.notificationsEnabled) { on ->
            viewModel.updateSettings { it.copy(notifyMilitary = on) }
        }
        ToggleRow("Medevac", settings.notifyMedevac, settings.notificationsEnabled) { on ->
            viewModel.updateSettings { it.copy(notifyMedevac = on) }
        }
        ToggleRow("Commercial", settings.notifyCommercial, settings.notificationsEnabled) { on ->
            viewModel.updateSettings { it.copy(notifyCommercial = on) }
        }
        ToggleRow("Private / Other", settings.notifyPrivate, settings.notificationsEnabled) { on ->
            viewModel.updateSettings { it.copy(notifyPrivate = on) }
        }
        ToggleRow("POI aircraft (overrides class filter)", settings.notifyPoi, settings.notificationsEnabled) { on ->
            viewModel.updateSettings { it.copy(notifyPoi = on) }
        }
        ToggleRow("Emergency squawk (7500/7600/7700)", settings.notifyEmergencySquawk, settings.notificationsEnabled) { on ->
            viewModel.updateSettings { it.copy(notifyEmergencySquawk = on) }
        }

        Button(
            onClick = { viewModel.sendTestNotification() },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("Send test notification") }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text, fontSize = 18.sp, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onBackground)
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
