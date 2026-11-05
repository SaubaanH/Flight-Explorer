package travel.roame.takehome.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.mutableFloatStateOf
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

private val SearchSpring = spring<Rect>(dampingRatio = 0.85f, stiffness = 400f)

@OptIn(ExperimentalSharedTransitionApi::class)
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
    var isSearching by remember { mutableStateOf(false) }
    var originText by remember { mutableStateOf(filters.originQuery) }
    var destText by remember { mutableStateOf(filters.destinationQuery) }
    var pointsSlider by remember {
        mutableFloatStateOf((filters.maxPoints ?: 133000).toFloat())
    }

    fun openSearch() { isSearching = true }
    fun closeSearch() { isSearching = false }
    fun resetAll() {
        originText = ""
        destText = ""
        pointsSlider = 133000f
        explorer.resetFilters()
    }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
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
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    SearchPill(
                        summary = searchSummary(filters),
                        detail = searchDetail(filters),
                        onClick = { openSearch() },
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "searchCard"),
                            animatedVisibilityScope = this,
                            boundsTransform = { _, _ -> SearchSpring },
                        ),
                    )
                }
                if (isSearching) {
                    Spacer(Modifier.height(64.dp))
                }

                ActiveFilterChips(
                    filters = filters,
                    resultCount = filteredFlights.size,
                    onReset = ::resetAll,
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

                    if (!isSearching) {
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

            AnimatedVisibility(
                visible = isSearching,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                val panelScope = this
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.15f))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { closeSearch() },
                            ),
                    )
                    SearchPanel(
                        filters = filters,
                        originText = originText,
                        destText = destText,
                        pointsSlider = pointsSlider,
                        onOriginChange = {
                            originText = it
                            explorer.setOriginQuery(it)
                        },
                        onDestChange = {
                            destText = it
                            explorer.setDestinationQuery(it)
                        },
                        onDateFrom = { explorer.setDateRange(it, filters.dateTo) },
                        onDateTo = { explorer.setDateRange(filters.dateFrom, it) },
                        onCabin = explorer::setCabin,
                        onPointsSlider = { pointsSlider = it },
                        onPointsFinished = { explorer.setMaxPoints(pointsSlider.toInt()) },
                        onTogglePointsFilter = {
                            if (filters.maxPoints == null) {
                                explorer.setMaxPoints(pointsSlider.toInt())
                            } else {
                                explorer.setMaxPoints(null)
                            }
                        },
                        onToggleProgram = explorer::toggleProgram,
                        onReset = ::resetAll,
                        onClose = { closeSearch() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "searchCard"),
                                animatedVisibilityScope = panelScope,
                                boundsTransform = { _, _ -> SearchSpring },
                            ),
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
private fun WindowedFlightList(
    flights: List<Flight>,
    selectedFlight: Flight?,
    savedIds: Set<String>,
    onSelect: (Flight) -> Unit,
) {
    var visibleCount by remember { mutableIntStateOf(FlightExplorer.LIST_PAGE_SIZE) }
    val listState = rememberLazyListState()
    // Reset the window whenever the filtered set changes (new search / filters).
    LaunchedEffect(flights) {
        visibleCount = min(FlightExplorer.LIST_PAGE_SIZE, flights.size)
        listState.scrollToItem(0)
    }
    val shown = remember(flights, visibleCount) { flights.take(visibleCount) }

    // Grow the window only when the user scrolls near the end (avoids LazyColumn prefetch
    // eagerly composing a footer LaunchedEffect that would load all 1000 rows).
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
