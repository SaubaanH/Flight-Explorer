package travel.roame.takehome.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import travel.roame.takehome.FlightExplorer
import travel.roame.takehome.model.DestinationCluster
import travel.roame.takehome.model.Flight
import travel.roame.takehome.model.FlightFilters
import travel.roame.takehome.model.LoadStatus
import kotlin.math.min

private enum class DiscoverPane { List, Map }

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
    var pane by remember { mutableStateOf(DiscoverPane.List) }

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
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                            contentScale = ContentScale.FillBounds,
                            alignment = Alignment.TopCenter,
                        ),
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
                            WindowedFlightList(
                                flights = filteredFlights,
                                selectedFlight = selectedFlight,
                                savedIds = savedIds,
                                onSelect = { explorer.selectFlight(it.id) },
                            )
                        }
                    }
                    DiscoverPane.Map -> {
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
private fun WindowedFlightList(
    flights: List<Flight>,
    selectedFlight: Flight?,
    savedIds: Set<String>,
    onSelect: (Flight) -> Unit,
) {
    var visibleCount by remember { mutableIntStateOf(FlightExplorer.LIST_PAGE_SIZE) }
    val listState = rememberLazyListState()
    LaunchedEffect(flights) {
        visibleCount = min(FlightExplorer.LIST_PAGE_SIZE, flights.size)
        listState.scrollToItem(0)
    }
    val shown = remember(flights, visibleCount) { flights.take(visibleCount) }

    LaunchedEffect(listState, shown.size, flights.size, visibleCount) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false
            visibleCount < flights.size && lastVisible >= shown.lastIndex - 2
        }.collect { nearEnd ->
            if (nearEnd) {
                visibleCount = min(visibleCount + FlightExplorer.LIST_PAGE_SIZE, flights.size)
            }
        }
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 160.dp),
    ) {
        items(
            items = shown,
            key = { it.id },
            contentType = { "flight" },
        ) { flight ->
            FlightRow(
                flight = flight,
                selected = flight.id == selectedFlight?.id,
                saved = flight.id in savedIds,
                onClick = { onSelect(flight) },
            )
        }
        if (visibleCount < flights.size) {
            item(key = "load-more") {
                Text(
                    "Showing $visibleCount of ${flights.size}…",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    color = RoameColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
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
            Surface(
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
