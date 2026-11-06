package travel.roame.takehome.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import travel.roame.takehome.FlightExplorer
import travel.roame.takehome.model.DestinationCluster
import travel.roame.takehome.model.Flight
import travel.roame.takehome.model.FlightFilters
import travel.roame.takehome.model.LoadStatus

internal val SearchSharedSpring = spring<Rect>(dampingRatio = 0.85f, stiffness = 400f)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.DiscoveryScreen(
    filters: FlightFilters,
    filteredFlights: List<Flight>,
    clusters: List<DestinationCluster>,
    selectedFlight: Flight?,
    savedIds: Set<String>,
    status: LoadStatus,
    browsingCluster: DestinationCluster?,
    explorer: FlightExplorer,
    isSearching: Boolean,
    onOpenSearch: () -> Unit,
    onClearOrigin: () -> Unit,
    onClearDestination: () -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    fun toggleSelect(flight: Flight) {
        if (selectedFlight?.id == flight.id) {
            explorer.selectFlight(null)
        } else {
            explorer.selectFlight(flight.id)
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusBanner(status = status, onRetry = { explorer.refresh() })

            RoameBrandHeader(
                title = "Discover",
                subtitle = "Find award flights with points",
            )

            AnimatedVisibility(
                visible = !isSearching,
                // None: let sharedBounds drive the pill→page morph (fade makes it look top-grown).
                enter = EnterTransition.None,
                exit = ExitTransition.None,
            ) {
                SearchPill(
                    summary = searchSummary(filters),
                    detail = searchDetail(filters),
                    onClick = onOpenSearch,
                    modifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "searchCard"),
                        animatedVisibilityScope = this,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                        boundsTransform = { _, _ -> SearchSharedSpring },
                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                        clipInOverlayDuringTransition = OverlayClip(RoameShapes.Panel),
                    ),
                )
            }
            if (isSearching) {
                Spacer(Modifier.height(64.dp))
            }

            ActiveFilterChips(
                filters = filters,
                resultCount = filteredFlights.size,
                onReset = onResetFilters,
                onClearOrigin = onClearOrigin,
                onClearDestination = onClearDestination,
                onClearCabin = { explorer.setCabin(null) },
                onClearMaxPoints = { explorer.setMaxPoints(null) },
                onClearDate = {
                    explorer.setDateRange(
                        FlightFilters.DEFAULT_DATE_FROM,
                        FlightFilters.DEFAULT_DATE_TO,
                    )
                },
                onRemoveProgram = explorer::toggleProgram,
            )

            // Stacked: map always above the list (no List/Map tabs).
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .border(1.dp, RoameColors.Hairline, RoameShapes.Card),
            ) {
                FlightMap(
                    clusters = clusters,
                    selectedFlight = selectedFlight,
                    onClusterTap = explorer::openCluster,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    filteredFlights.isEmpty() && status == LoadStatus.Loading -> {
                        Text(
                            "Loading flights…",
                            modifier = Modifier.align(Alignment.Center),
                            color = RoameColors.Muted,
                        )
                    }
                    filteredFlights.isEmpty() && status !is LoadStatus.Error -> {
                        val emptyCopy = when (status) {
                            LoadStatus.Empty -> "No flights returned from the server."
                            else -> "No flights match these filters."
                        }
                        Text(
                            emptyCopy,
                            modifier = Modifier.align(Alignment.Center),
                            color = RoameColors.Muted,
                        )
                    }
                    filteredFlights.isEmpty() -> {
                        // Error with no rows — banner above already explains + Retry.
                    }
                    else -> {
                        key(
                            filteredFlights.size,
                            filteredFlights.firstOrNull()?.id,
                            filteredFlights.lastOrNull()?.id,
                        ) {
                            FlightResultList(
                                flights = filteredFlights,
                                selectedFlight = selectedFlight,
                                savedIds = savedIds,
                                onSelect = ::toggleSelect,
                                onSave = explorer::saveFlight,
                                onRemove = explorer::removeSaved,
                            )
                        }
                    }
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
                                toggleSelect(flight)
                                explorer.dismissCluster()
                            },
                            onSave = { explorer.saveFlight(flight) },
                            onRemove = { explorer.removeSaved(flight.id) },
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
private fun FlightResultList(
    flights: List<Flight>,
    selectedFlight: Flight?,
    savedIds: Set<String>,
    onSelect: (Flight) -> Unit,
    onSave: (Flight) -> Unit,
    onRemove: (String) -> Unit,
) {
    LazyColumn(
        state = rememberLazyListState(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {
        items(
            items = flights,
            key = { it.id },
            contentType = { "flight" },
        ) { flight ->
            FlightRow(
                flight = flight,
                selected = flight.id == selectedFlight?.id,
                saved = flight.id in savedIds,
                onClick = { onSelect(flight) },
                onSave = { onSave(flight) },
                onRemove = { onRemove(flight.id) },
            )
        }
    }
}
