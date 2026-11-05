package travel.roame.takehome.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import travel.roame.takehome.model.Flight
import travel.roame.takehome.model.FlightFilters
import travel.roame.takehome.model.LoadStatus

@Composable
fun StatusBanner(
    status: LoadStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (status) {
        LoadStatus.Loading -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Text(
                    "Loading flights…",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        LoadStatus.OfflineStale -> {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Offline stale data banner" },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Offline — showing cached flights",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
            }
        }
        is LoadStatus.Error -> {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Error loading flights" },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(status.message, style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = onRetry) { Text("Retry") }
                }
            }
        }
        else -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveFilterChips(
    filters: FlightFilters,
    resultCount: Int,
    onReset: () -> Unit,
    onClearOrigin: () -> Unit,
    onClearDestination: () -> Unit,
    onClearCabin: () -> Unit,
    onClearMaxPoints: () -> Unit,
    onClearDate: () -> Unit,
    onRemoveProgram: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$resultCount flights",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { contentDescription = "Result count $resultCount" },
            )
            if (!filters.isDefault()) {
                TextButton(onClick = onReset) { Text("Reset filters") }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (filters.originQuery.isNotBlank()) {
                AssistChip(
                    onClick = onClearOrigin,
                    label = { Text("From: ${filters.originQuery.trim()}") },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Clear origin filter") },
                )
            }
            if (filters.destinationQuery.isNotBlank()) {
                AssistChip(
                    onClick = onClearDestination,
                    label = { Text("To: ${filters.destinationQuery.trim()}") },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Clear destination filter") },
                )
            }
            if (filters.dateFrom != FlightFilters.DEFAULT_DATE_FROM ||
                filters.dateTo != FlightFilters.DEFAULT_DATE_TO
            ) {
                AssistChip(
                    onClick = onClearDate,
                    label = { Text("${filters.dateFrom} → ${filters.dateTo}") },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Clear date filter") },
                )
            }
            filters.cabin?.let { cabin ->
                AssistChip(
                    onClick = onClearCabin,
                    label = { Text(cabin) },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Clear cabin filter") },
                )
            }
            filters.maxPoints?.let { max ->
                AssistChip(
                    onClick = onClearMaxPoints,
                    label = { Text("≤ ${"%,d".format(max)} pts") },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Clear points filter") },
                )
            }
            filters.programs.forEach { program ->
                AssistChip(
                    onClick = { onRemoveProgram(program) },
                    label = { Text(program) },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "Remove $program filter")
                    },
                )
            }
        }
    }
}

@Composable
fun FlightRow(
    flight: Flight,
    selected: Boolean,
    saved: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "Flight ${flight.origin} to ${flight.destination} on ${flight.departureDate}, ${flight.points} points"
            },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${flight.origin} → ${flight.destination}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${flight.originCity} to ${flight.destinationCity}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "${flight.departureDate} · ${flight.cabin} · ${flight.program}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "%,d".format(flight.points),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    imageVector = if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (saved) "Saved flight" else "Not saved",
                    tint = if (saved) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
fun SelectionCard(
    flight: Flight,
    saved: Boolean,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Selected flight details" },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${flight.origin} → ${flight.destination}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Clear selection")
                }
            }
            Text("${flight.originCity} → ${flight.destinationCity}")
            Text("Depart ${flight.departureDate}")
            Text("${flight.cabin} · ${flight.program}")
            Text("%,d points".format(flight.points), fontWeight = FontWeight.SemiBold)
            if (saved) {
                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier.semantics { contentDescription = "Remove saved flight" },
                ) { Text("Remove") }
            } else {
                OutlinedButton(
                    onClick = onSave,
                    modifier = Modifier.semantics { contentDescription = "Save flight" },
                ) { Text("Save") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgramChips(
    selected: Set<String>,
    programs: List<String>,
    onToggle: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        programs.forEach { program ->
            FilterChip(
                selected = program in selected,
                onClick = { onToggle(program) },
                label = { Text(program) },
                modifier = Modifier.semantics {
                    contentDescription = "Loyalty program $program"
                },
            )
        }
    }
}
