package travel.roame.takehome.android.ui

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
@Suppress("UnusedImport")
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

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatusBanner(status = status, onRetry = { explorer.refresh() })

        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Discover", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = originText,
                onValueChange = {
                    originText = it
                    explorer.setOriginQuery(it)
                },
                label = { Text("Origin") },
                placeholder = { Text("Code or city") },
                singleLine = true,
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
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Departure date from" },
                )
                OutlinedTextField(
                    value = filters.dateTo,
                    onValueChange = { explorer.setDateRange(filters.dateFrom, it) },
                    label = { Text("To date") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Departure date to" },
                )
            }
            Text("Cabin", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filters.cabin == null,
                    onClick = { explorer.setCabin(null) },
                    label = { Text("Any") },
                    modifier = Modifier.semantics { contentDescription = "Cabin filter any" },
                )
                Cabin.allLabels.forEach { cabin ->
                    FilterChip(
                        selected = filters.cabin == cabin,
                        onClick = { explorer.setCabin(cabin) },
                        label = { Text(cabin) },
                        modifier = Modifier.semantics { contentDescription = "Cabin filter $cabin" },
                    )
                }
            }
            Text(
                "Max points: ${"%,d".format(pointsSlider.toInt())}",
                modifier = Modifier.semantics { contentDescription = "Maximum points filter" },
            )
            Slider(
                value = pointsSlider,
                onValueChange = { pointsSlider = it },
                valueRange = 9000f..133000f,
                onValueChangeFinished = { explorer.setMaxPoints(pointsSlider.toInt()) },
                modifier = Modifier.semantics { contentDescription = "Maximum points slider" },
            )
            FilterChip(
                selected = filters.maxPoints != null,
                onClick = {
                    if (filters.maxPoints == null) {
                        explorer.setMaxPoints(pointsSlider.toInt())
                    } else {
                        explorer.setMaxPoints(null)
                    }
                },
                label = {
                    Text(if (filters.maxPoints == null) "Points filter off" else "Points filter on")
                },
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

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = pane == DiscoverPane.List,
                onClick = { pane = DiscoverPane.List },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                label = { Text("List") },
            )
            SegmentedButton(
                selected = pane == DiscoverPane.Map,
                onClick = { pane = DiscoverPane.Map },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                label = { Text("Map") },
            )
        }

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
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 140.dp),
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
                    FlightMap(
                        clusters = clusters,
                        selectedFlight = selectedFlight,
                        onClusterTap = explorer::openCluster,
                        modifier = Modifier.fillMaxSize(),
                    )
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
            title = { Text("${cluster.destination} · ${cluster.flightCount} flights") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text(
                            "From ${"%,d".format(cluster.lowestPoints)} points · ${cluster.destinationCity}",
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
                TextButton(onClick = explorer::dismissCluster) { Text("Close") }
            },
        )
    }
}
