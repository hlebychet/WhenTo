package com.autocalendar.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autocalendar.data.ParsedMeeting
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
) {
    val items by viewModel.items.collectAsState()
    val zone = ZoneId.systemDefault()

    Column(Modifier.fillMaxSize()) {
        Text(
            "History",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )
        if (items.isEmpty()) {
            Text(
                "No saved meetings yet.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn {
                items(items, key = { it.id }) { item ->
                    HistoryRow(item, zone, onClick = { viewModel.select(item) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: ParsedMeeting,
    zone: ZoneId,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(item.title, style = MaterialTheme.typography.titleMedium)
        Text(
            Instant.ofEpochMilli(item.startMillis).atZone(zone).format(displayFormatter),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        item.location?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
