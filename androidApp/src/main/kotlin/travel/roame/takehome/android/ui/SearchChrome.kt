package travel.roame.takehome.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import travel.roame.takehome.model.Cabin
import travel.roame.takehome.model.FlightFilters
import travel.roame.takehome.model.LoyaltyPrograms

fun searchSummary(filters: FlightFilters): String {
    val origin = filters.originQuery.trim().ifEmpty { "Anywhere" }
    val dest = filters.destinationQuery.trim().ifEmpty { "Anywhere" }
    return "$origin → $dest · ${shortDate(filters.dateFrom)} – ${shortDate(filters.dateTo)}"
}

fun searchDetail(filters: FlightFilters): String {
    val parts = buildList {
        filters.cabin?.let { add(it) }
        filters.maxPoints?.let { add("≤ ${"%,d".format(it)} pts") }
        if (filters.programs.isNotEmpty()) add("${filters.programs.size} programs")
    }
    return parts.ifEmpty { listOf("Tap to edit filters") }.joinToString(" · ")
}

private fun shortDate(iso: String): String {
    val parts = iso.split("-")
    if (parts.size != 3) return iso
    val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    val month = months.getOrElse((parts[1].toIntOrNull() ?: 1) - 1) { "Jan" }
    val day = parts[2].toIntOrNull() ?: 1
    return "$month $day"
}

@Composable
fun SearchPill(
    summary: String,
    detail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoameShapes.Panel,
        color = RoameColors.Surface,
        border = BorderStroke(1.dp, RoameColors.Hairline),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Open search filters" },
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = RoameColors.Pink,
                modifier = Modifier.size(20.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    summary,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = RoameColors.Muted,
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchPanel(
    filters: FlightFilters,
    originText: String,
    destText: String,
    pointsSlider: Float,
    onOriginChange: (String) -> Unit,
    onDestChange: (String) -> Unit,
    onDateFrom: (String) -> Unit,
    onDateTo: (String) -> Unit,
    onCabin: (String?) -> Unit,
    onPointsSlider: (Float) -> Unit,
    onPointsFinished: () -> Unit,
    onTogglePointsFilter: () -> Unit,
    onToggleProgram: (String) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = RoameColors.Pink,
        unfocusedBorderColor = RoameColors.Hairline,
        focusedLabelColor = RoameColors.Pink,
        cursorColor = RoameColors.Pink,
        focusedContainerColor = RoameColors.Canvas,
        unfocusedContainerColor = RoameColors.Canvas,
    )

    Column(
        modifier
            .fillMaxSize()
            .clip(RoameShapes.Panel)
            .background(RoameColors.Surface)
            .border(1.dp, RoameColors.Hairline, RoameShapes.Panel),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = onClose,
                shape = CircleShape,
                color = Color(0xFFF0F0F0),
                modifier = Modifier
                    .size(36.dp)
                    .semantics { contentDescription = "Close search" },
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = RoameColors.Ink,
                )
            }
            Text(
                "Search flights",
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
            )
            Spacer(Modifier.size(36.dp))
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = originText,
                onValueChange = onOriginChange,
                label = { Text("Origin") },
                placeholder = { Text("Code or city") },
                singleLine = true,
                shape = RoameShapes.Pill,
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Origin search field" },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            )
            OutlinedTextField(
                value = destText,
                onValueChange = onDestChange,
                label = { Text("Destination") },
                placeholder = { Text("Code or city") },
                singleLine = true,
                shape = RoameShapes.Pill,
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Destination search field" },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = filters.dateFrom,
                    onValueChange = onDateFrom,
                    label = { Text("From date") },
                    singleLine = true,
                    shape = RoameShapes.Pill,
                    colors = fieldColors,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Departure date from" },
                )
                OutlinedTextField(
                    value = filters.dateTo,
                    onValueChange = onDateTo,
                    label = { Text("To date") },
                    singleLine = true,
                    shape = RoameShapes.Pill,
                    colors = fieldColors,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Departure date to" },
                )
            }

            Text("Cabin", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RoameFilterChip(
                    selected = filters.cabin == null,
                    label = "Any",
                    onClick = { onCabin(null) },
                    contentDescription = "Cabin filter any",
                )
                Cabin.allLabels.forEach { cabin ->
                    RoameFilterChip(
                        selected = filters.cabin == cabin,
                        label = cabin,
                        onClick = { onCabin(cabin) },
                        contentDescription = "Cabin filter $cabin",
                    )
                }
            }

            Text(
                "Max points: ${"%,d".format(pointsSlider.toInt())}",
                modifier = Modifier.semantics { contentDescription = "Maximum points filter" },
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = pointsSlider,
                onValueChange = onPointsSlider,
                valueRange = 9000f..133000f,
                onValueChangeFinished = onPointsFinished,
                colors = SliderDefaults.colors(
                    thumbColor = RoameColors.Pink,
                    activeTrackColor = RoameColors.Pink,
                    inactiveTrackColor = RoameColors.PinkSoft,
                ),
                modifier = Modifier.semantics { contentDescription = "Maximum points slider" },
            )
            RoameFilterChip(
                selected = filters.maxPoints != null,
                label = if (filters.maxPoints == null) "Points filter off" else "Points filter on",
                onClick = onTogglePointsFilter,
                contentDescription = "Points filter toggle",
            )

            Text("Loyalty programs", style = MaterialTheme.typography.labelLarge)
            ProgramChips(
                selected = filters.programs,
                programs = LoyaltyPrograms.all,
                onToggle = onToggleProgram,
            )

            TextButton(onClick = onReset) {
                Text("Reset filters", color = RoameColors.Pink, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }

        RoamePrimaryButton(
            label = "Search Flights",
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics { contentDescription = "Apply search" },
        )
    }
}
