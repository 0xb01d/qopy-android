package com.qopy.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ControlTogglesSection(
    autoSync: Boolean,
    onAutoSyncChange: (Boolean) -> Unit,
    filterSensitive: Boolean,
    onFilterSensitiveChange: (Boolean) -> Unit,
    isPaused: Boolean,
    onIsPausedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            ToggleItem(
                title = "Auto-Sync to Clipboard",
                description = "Automatically updates OS clipboard on receive",
                checked = autoSync,
                onCheckedChange = onAutoSyncChange
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            ToggleItem(
                title = "Filter Sensitive Data",
                description = "Shields OTPs, API keys, passwords from sync preview",
                checked = filterSensitive,
                onCheckedChange = onFilterSensitiveChange
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            ToggleItem(
                title = "Pause Synchronization",
                description = "Temporarily halts broadcast & receiving",
                checked = isPaused,
                onCheckedChange = onIsPausedChange
            )
        }
    }
}

@Composable
private fun ToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
