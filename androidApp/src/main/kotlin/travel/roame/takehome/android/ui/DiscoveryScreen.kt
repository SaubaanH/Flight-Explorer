package travel.roame.takehome.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import travel.roame.takehome.FlightExplorer
import travel.roame.takehome.model.Cabin
import travel.roame.takehome.model.DestinationCluster
import travel.roame.takehome.model.Flight
import travel.roame.takehome.model.FlightFilters
import travel.roame.takehome.model.LoadStatus
import travel.roame.takehome.model.LoyaltyPrograms

private enum class DiscoverPane { List, Map }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiscoveryScreen(
    filters: FlightFilters,
    filteredFlights: List<Flight>,
    clusters: List<DestinationCluster>,
    selectedFlight: Flight?,
    savedIds: Set<String>,
    status: LoadStatus,
    browsingCluster: DestinationCluster?,
    explorer: FlightExplorer,
    modifier: Modifier = Modifier,
) {
    var pane by remember { mutableStateOf(DiscoverPane.List) }
    var originText by remember { mutableStateOf(filters.originQuery) }
    var destText by remember { mutableStateOf(filters.destinationQuery) }
    var pointsSlider by remember {
        mutableFloatStateOf((filters.maxPoints ?: 133000).toFloat())
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = RoameColors.Pink,
        unfocusedBorderColor = RoameColors.Hairline,
        focusedLabelColor = RoameColors.Pink,
        cursorColor = RoameColors.Pink,
        focusedContainerColor = RoameColors.Surface,
        unfocusedContainerColor = RoameColors.Surface,
    )

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusBanner(status = status, onRetry = { explorer.refresh() })

        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
                .clip(RoameShapes.Panel)
                .background(RoameColors.Surface)
                .border(1.dp, RoameColors.Hairline, RoameShapes.Panel)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RoameBrandHeader(
                title = "Discover",
                subtitle = "Find award flights with points",
            )
            OutlinedTextField(
                value = originText,
                onValueChange = {
                    originText = it
                    explorer.setOriginQuery(it)
                },
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
                onValueChange = {
                    destText = it
                    explorer.setDestinationQuery(it)
                },
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
                    onValueChange = { explorer.setDateRange(it, filters.dateTo) },
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
                    onValueChange = { explorer.setDateRange(filters.dateFrom, it) },
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
                    onClick = { explorer.setCabin(null) },
                    contentDescription = "Cabin filter any",
                )
                Cabin.allLabels.forEach { cabin ->
                    RoameFilterChip(
                        selected = filters.cabin == cabin,
                        label = cabin,
                        onClick = { explorer.setCabin(cabin) },
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
                onValueChange = { pointsSlider = it },
                valueRange = 9000f..133000f,
                onValueChangeFinished = { explorer.setMaxPoints(pointsSlider.toInt()) },
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
                onClick = {
                    if (filters.maxPoints == null) {
                        explorer.setMaxPoints(pointsSlider.toInt())
                    } else {
                        explorer.setMaxPoints(null)
                    }
                },
                contentDescription = "Points filter toggle",
            )
            Text("Loyalty programs", style = MaterialTheme.typography.labelLarge)
            ProgramChips(
                selected = filters.programs,
                programs = LoyaltyPrograms.all,
                onToggle = explorer::toggleProgram,
            )
        }

        ActiveFilterChips(
            filters = filters,
            resultCount = filteredFlights.size,
            onReset = {
                originText = ""
                destText = ""
                pointsSlider = 133000f
                explorer.resetFilters()
            },
            onClearOrigin = {
                originText = ""
                explorer.setOriginQuery("")
            },
            onClearDestination = {
                destText = ""
                explorer.setDestinationQuery("")
            },
            onClearCabin = { explorer.setCabin(null) },
            onClearMaxPoints = { explorer.setMaxPoints(null) },
            onClearDate = {
                explorer.setDateRange(FlightFilters.DEFAULT_DATE_FROM, FlightFilters.DEFAULT_DATE_TO)
            },
            onRemoveProgram = explorer::toggleProgram,
        )

        PaneToggle(pane = pane, onPane = { pane = it })

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (pane) {
                DiscoverPane.List -> {
                    if (filteredFlights.isEmpty() &&
                        status !is LoadStatus.Error &&
                        status != LoadStatus.Loading
                    ) {
                        Text(
                            "No flights match these filters.",
                            modifier = Modifier.align(Alignment.Center),
                            color = RoameColors.Muted,
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 160.dp),
                        ) {
                            items(filteredFlights, key = { it.id }) { flight ->
                                FlightRow(
                                    flight = flight,
                                    selected = flight.id == selectedFlight?.id,
                                    saved = flight.id in savedIds,
                                    onClick = { explorer.selectFlight(flight.id) },
                                )
                            }
                        }
                    }
                }
                DiscoverPane.Map -> {
                    // Don't clip the WebView parent — Compose clip() blanks MapLibre/WebView.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .border(1.dp, RoameColors.Hairline, RoameShapes.Card),
                    ) {
                        FlightMap(
                            clusters = clusters,
                            selectedFlight = selectedFlight,
                            onClusterTap = explorer::openCluster,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            selectedFlight?.let { flight ->
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .fillMaxWidth(),
                ) {
                    SelectionCard(
                        flight = flight,
                        saved = flight.id in savedIds,
                        onSave = { explorer.saveFlight(flight) },
                        onRemove = { explorer.removeSaved(flight.id) },
                        onDismiss = { explorer.selectFlight(null) },
                    )
                }
            }
        }
    }

    browsingCluster?.let { cluster ->
        AlertDialog(
            onDismissRequest = explorer::dismissCluster,
            containerColor = RoameColors.Surface,
            shape = RoameShapes.Panel,
            title = {
                Text(
                    "${cluster.destination} · ${cluster.flightCount} flights",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text(
                            "From ${"%,d".format(cluster.lowestPoints)} points · ${cluster.destinationCity}",
                            color = RoameColors.Muted,
                        )
                    }
                    items(cluster.flights, key = { it.id }) { flight ->
                        FlightRow(
                            flight = flight,
                            selected = flight.id == selectedFlight?.id,
                            saved = flight.id in savedIds,
                            onClick = {
                                explorer.selectFlight(flight.id)
                                explorer.dismissCluster()
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = explorer::dismissCluster) {
                    Text("Close", color = RoameColors.Pink, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun PaneToggle(pane: DiscoverPane, onPane: (DiscoverPane) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoameShapes.Pill)
            .background(RoameColors.Surface)
            .border(1.dp, RoameColors.Hairline, RoameShapes.Pill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DiscoverPane.entries.forEach { option ->
            val selected = pane == option
            androidx.compose.material3.Surface(
                onClick = { onPane(option) },
                shape = RoameShapes.Pill,
                color = if (selected) RoameColors.PinkSoft else Color.Transparent,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    option.name,
                    Modifier
                        .padding(vertical = 10.dp)
                        .fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = if (selected) RoameColors.Pink else RoameColors.Muted,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
