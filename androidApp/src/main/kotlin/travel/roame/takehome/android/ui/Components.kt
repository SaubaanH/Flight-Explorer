package travel.roame.takehome.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import travel.roame.takehome.model.Flight
import travel.roame.takehome.model.FlightFilters
import travel.roame.takehome.model.LoadStatus

@Composable
fun RoameBrandHeader(
    title: String,
    subtitle: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "roame",
            color = RoameColors.Pink,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = (-0.5).sp,
        )
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = RoameColors.Muted)
        }
    }
}

@Composable
fun StatusBanner(
    status: LoadStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (status) {
        LoadStatus.Loading -> {
            BannerCard(modifier, RoameColors.PinkSoft) {
                Text("Loading flights…", style = MaterialTheme.typography.bodyMedium)
            }
        }
        LoadStatus.OfflineStale -> {
            BannerCard(
                modifier.semantics { contentDescription = "Offline stale data banner" },
                RoameColors.Offline,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Offline — showing cached flights",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onRetry) {
                        Text("Retry", color = RoameColors.Pink, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        is LoadStatus.Error -> {
            BannerCard(
                modifier.semantics { contentDescription = "Error loading flights" },
                RoameColors.ErrorSoft,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(status.message, style = MaterialTheme.typography.bodyMedium)
                    RoamePrimaryButton(label = "Retry", onClick = onRetry)
                }
            }
        }
        LoadStatus.Empty -> {
            BannerCard(
                modifier.semantics { contentDescription = "Empty flights response" },
                RoameColors.PinkSoft,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "No flights returned from the server.",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onRetry) {
                        Text("Retry", color = RoameColors.Pink, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        else -> Unit
    }
}

@Composable
private fun BannerCard(
    modifier: Modifier,
    color: Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoameShapes.Card)
            .background(color)
            .padding(14.dp),
    ) { content() }
}

@Composable
fun RoamePrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
) {
    // Flat pill matching iOS `RoamePillButton` — avoid Material3 Button tonal remapping.
    val bg = if (filled) RoameColors.Pink else Color.Transparent
    val fg = if (filled) Color.White else RoameColors.Pink
    Box(
        modifier
            .height(44.dp)
            .clip(RoameShapes.Pill)
            .background(bg)
            .then(
                if (filled) Modifier
                else Modifier.border(1.dp, RoameColors.Pink, RoameShapes.Pill),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveFilterChips(
    filters: FlightFilters,
    resultCount: Int,
    mapFocusAirport: String? = null,
    onReset: () -> Unit,
    onClearOrigin: () -> Unit,
    onClearDestination: () -> Unit,
    onClearCabin: () -> Unit,
    onClearMaxPoints: () -> Unit,
    onClearDate: () -> Unit,
    onRemoveProgram: (String) -> Unit,
    onClearMapFocus: () -> Unit = {},
) {
    val hasExtra = mapFocusAirport != null
    val hasChips =
        filters.originQuery.isNotBlank() ||
            filters.destinationQuery.isNotBlank() ||
            filters.dateFrom != FlightFilters.DEFAULT_DATE_FROM ||
            filters.dateTo != FlightFilters.DEFAULT_DATE_TO ||
            filters.cabin != null ||
            filters.maxPoints != null ||
            filters.programs.isNotEmpty() ||
            mapFocusAirport != null

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$resultCount flights",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.semantics { contentDescription = "Result count $resultCount" },
            )
            if (!filters.isDefault() || hasExtra) {
                Text(
                    "Reset",
                    color = RoameColors.Pink,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onReset,
                        )
                        .padding(vertical = 2.dp, horizontal = 4.dp),
                )
            }
        }
        if (hasChips) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (filters.originQuery.isNotBlank()) {
                    RemovableChip("From: ${filters.originQuery.trim()}", onClearOrigin, "Clear origin filter")
                }
                if (filters.destinationQuery.isNotBlank()) {
                    RemovableChip("To: ${filters.destinationQuery.trim()}", onClearDestination, "Clear destination filter")
                }
                if (filters.dateFrom != FlightFilters.DEFAULT_DATE_FROM ||
                    filters.dateTo != FlightFilters.DEFAULT_DATE_TO
                ) {
                    RemovableChip("${filters.dateFrom} → ${filters.dateTo}", onClearDate, "Clear date filter")
                }
                filters.cabin?.let { cabin ->
                    RemovableChip(cabin, onClearCabin, "Clear cabin filter")
                }
                filters.maxPoints?.let { max ->
                    RemovableChip("≤ ${"%,d".format(max)} pts", onClearMaxPoints, "Clear points filter")
                }
                filters.programs.forEach { program ->
                    RemovableChip(program, { onRemoveProgram(program) }, "Remove $program filter")
                }
                mapFocusAirport?.let { code ->
                    RemovableChip("Map: $code", onClearMapFocus, "Clear map airport filter")
                }
            }
        }
    }
}

@Composable
private fun RemovableChip(label: String, onClick: () -> Unit, clearDescription: String) {
    Surface(
        onClick = onClick,
        shape = RoameShapes.Pill,
        color = RoameColors.PinkSoft,
        border = BorderStroke(1.dp, RoameColors.Pink.copy(alpha = 0.22f)),
        modifier = Modifier.semantics { contentDescription = clearDescription },
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = RoameColors.Pink)
            Icon(Icons.Default.Close, contentDescription = null, tint = RoameColors.Pink, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun FlightRow(
    flight: Flight,
    selected: Boolean,
    saved: Boolean,
    onClick: () -> Unit,
    onSave: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    // Flat Box + border is cheaper to scroll than Material Surface per row.
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoameShapes.Card)
            .background(if (selected) RoameColors.PinkSelected else RoameColors.Surface)
            .border(
                1.dp,
                if (selected) RoameColors.Pink.copy(alpha = 0.40f) else RoameColors.Hairline,
                RoameShapes.Card,
            )
            .semantics {
                contentDescription =
                    "Flight ${flight.origin} to ${flight.destination} on ${flight.departureDate}, ${flight.points} points"
            },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        flight.destinationCity,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = RoameColors.Ink,
                    )
                    Text(
                        "${flight.origin} → ${flight.destination}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoameColors.Muted,
                    )
                }
                Icon(
                    imageVector = if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (saved) "Saved flight" else "Not saved",
                    tint = if (saved) RoameColors.Pink else RoameColors.Muted,
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "%,d".format(flight.points),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = RoameColors.Ink,
                )
                Spacer(Modifier.width(6.dp))
                Text("pts", style = MaterialTheme.typography.bodyMedium, color = RoameColors.Muted)
                Spacer(Modifier.weight(1f))
                Text(
                    flight.cabin,
                    color = RoameColors.Pink,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(flight.departureDate, style = MaterialTheme.typography.bodySmall, color = RoameColors.Muted)
                Text(
                    flight.program,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = RoameColors.Muted,
                )
            }
        }

        AnimatedVisibility(visible = selected && (onSave != null || onRemove != null)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "${flight.originCity} → ${flight.destinationCity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = RoameColors.Muted,
                )
                if (saved && onRemove != null) {
                    RoamePrimaryButton(
                        label = "Remove",
                        onClick = onRemove,
                        filled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Remove saved flight" },
                    )
                } else if (onSave != null) {
                    RoamePrimaryButton(
                        label = "Save",
                        onClick = onSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Save flight" },
                    )
                }
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
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoameShapes.Panel, clip = false)
            .clip(RoameShapes.Panel)
            .background(RoameColors.Surface)
            .border(1.dp, RoameColors.Hairline, RoameShapes.Panel)
            .semantics { contentDescription = "Selected flight details" }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${flight.origin} → ${flight.destination}",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "${flight.originCity} → ${flight.destinationCity}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Clear selection", tint = RoameColors.Muted)
            }
        }
        Text("Depart ${flight.departureDate} · ${flight.cabin}", style = MaterialTheme.typography.bodyMedium)
        Text(flight.program, style = MaterialTheme.typography.bodySmall, color = RoameColors.Muted)
        Text(
            "%,d points".format(flight.points),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = RoameColors.Ink,
        )
        if (saved) {
            RoamePrimaryButton(
                label = "Remove",
                onClick = onRemove,
                filled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Remove saved flight" },
            )
        } else {
            RoamePrimaryButton(
                label = "Save",
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Save flight" },
            )
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
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        programs.forEach { program ->
            val isOn = program in selected
            Surface(
                onClick = { onToggle(program) },
                shape = RoameShapes.Pill,
                color = if (isOn) RoameColors.Pink else RoameColors.Surface,
                border = BorderStroke(1.dp, if (isOn) RoameColors.Pink else RoameColors.Hairline),
                modifier = Modifier.semantics { contentDescription = "Loyalty program $program" },
            ) {
                Text(
                    program,
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (isOn) Color.White else RoameColors.Ink,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
fun RoameFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    contentDescription: String,
) {
    Surface(
        onClick = onClick,
        shape = RoameShapes.Pill,
        color = if (selected) RoameColors.Pink else RoameColors.Surface,
        border = BorderStroke(1.dp, if (selected) RoameColors.Pink else RoameColors.Hairline),
        modifier = Modifier.semantics { this.contentDescription = contentDescription },
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = if (selected) Color.White else RoameColors.Ink,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
