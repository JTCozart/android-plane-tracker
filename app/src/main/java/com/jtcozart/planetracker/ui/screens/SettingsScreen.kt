package com.jtcozart.planetracker.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtcozart.planetracker.data.Settings
import com.jtcozart.planetracker.data.ThemeMode
import com.jtcozart.planetracker.ui.TrackerViewModel

@Composable
fun SettingsScreen(
    settings: Settings,
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    onNotificationsSectionPositioned: (Rect) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle("Appearance")

        ThemeDropdown(settings.themeMode) { mode ->
            viewModel.updateSettings { it.copy(themeMode = mode) }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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
            label = { Text("POI types") },
            supportingText = { Text("e.g. B737,F16,C172") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        ToggleRow("POI filter", "Show only listed types", settings.poiEnabled) { on ->
            viewModel.updateSettings { it.copy(poiEnabled = on) }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Column(
            modifier = Modifier.onGloballyPositioned { coords ->
                onNotificationsSectionPositioned(coords.boundsInRoot())
            },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle("Notifications")

            ToggleRow("Notifications", checked = settings.notificationsEnabled) { on ->
                viewModel.updateSettings { it.copy(notificationsEnabled = on) }
            }
            ToggleRow("Military", checked = settings.notifyMilitary, enabled = settings.notificationsEnabled) { on ->
                viewModel.updateSettings { it.copy(notifyMilitary = on) }
            }
            ToggleRow("Medevac", checked = settings.notifyMedevac, enabled = settings.notificationsEnabled) { on ->
                viewModel.updateSettings { it.copy(notifyMedevac = on) }
            }
            ToggleRow("Commercial", checked = settings.notifyCommercial, enabled = settings.notificationsEnabled) { on ->
                viewModel.updateSettings { it.copy(notifyCommercial = on) }
            }
            ToggleRow("Private/Other", checked = settings.notifyPrivate, enabled = settings.notificationsEnabled) { on ->
                viewModel.updateSettings { it.copy(notifyPrivate = on) }
            }
            ToggleRow("POI alerts", "Overrides class filter", settings.notifyPoi, settings.notificationsEnabled) { on ->
                viewModel.updateSettings { it.copy(notifyPoi = on) }
            }
            ToggleRow("Emergency squawk", "7500 / 7600 / 7700", settings.notifyEmergencySquawk, settings.notificationsEnabled) { on ->
                viewModel.updateSettings { it.copy(notifyEmergencySquawk = on) }
            }
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System Default"
    ThemeMode.DARK -> "Dark"
    ThemeMode.LIGHT -> "Light"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeDropdown(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Theme") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label()) },
                    onClick = {
                        onSelect(mode)
                        expanded = false
                    },
                )
            }
        }
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
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
