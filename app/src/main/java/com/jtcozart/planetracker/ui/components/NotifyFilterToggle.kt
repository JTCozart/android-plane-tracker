package com.jtcozart.planetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** "All" / "Notify matches" segmented filter shared by the Live and Map screens. */
@Composable
fun NotifyFilterToggle(
    showOnlyNotifyMatches: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
) {
    val colors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surface,
        labelColor = MaterialTheme.colorScheme.onSurface,
    )
    Row(modifier = modifier, horizontalArrangement = horizontalArrangement) {
        FilterChip(
            selected = !showOnlyNotifyMatches,
            onClick = { onChange(false) },
            label = { Text("All") },
            colors = colors,
        )
        FilterChip(
            selected = showOnlyNotifyMatches,
            onClick = { onChange(true) },
            label = { Text("Filtered matches") },
            colors = colors,
        )
    }
}
