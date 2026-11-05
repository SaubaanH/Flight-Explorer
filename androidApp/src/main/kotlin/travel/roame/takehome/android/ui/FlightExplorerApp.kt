package travel.roame.takehome.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import travel.roame.takehome.FlightExplorer

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FlightExplorerApp(explorer: FlightExplorer) {
    var tab by remember { mutableIntStateOf(0) }
    var isSearching by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val filters by explorer.filters.collectAsState()
    val filtered by explorer.filteredFlights.collectAsState()
    val clusters by explorer.clusters.collectAsState()
    val selected by explorer.selectedFlight.collectAsState()
    val saved by explorer.savedFlights.collectAsState()
    val savedIds by explorer.savedIds.collectAsState()
    val status by explorer.status.collectAsState()
    val browsing by explorer.browsingCluster.collectAsState()
    val saveError by explorer.saveError.collectAsState()

    var originText by remember { mutableStateOf(filters.originQuery) }
    var destText by remember { mutableStateOf(filters.destinationQuery) }
    var pointsSlider by remember {
        mutableFloatStateOf((filters.maxPoints ?: 133000).toFloat())
    }

    fun resetAll() {
        originText = ""
        destText = ""
        pointsSlider = 133000f
        explorer.resetFilters()
    }

    BackHandler(enabled = isSearching) {
        isSearching = false
    }

    LaunchedEffect(saveError) {
        val message = saveError ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        explorer.clearSaveError()
    }

    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            // Keep the bottom bar in the Scaffold layout always so Discover padding
            // (and the search pill bounds) stay stable during the shared morph.
            Scaffold(
                containerColor = RoameColors.Canvas,
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    RoameBottomBar(
                        tab = tab,
                        onTab = { tab = it },
                    )
                },
            ) { padding ->
                when (tab) {
                    0 -> DiscoveryScreen(
                        filters = filters,
                        filteredFlights = filtered,
                        clusters = clusters,
                        selectedFlight = selected,
                        savedIds = savedIds,
                        status = status,
                        browsingCluster = browsing,
                        explorer = explorer,
                        isSearching = isSearching,
                        onOpenSearch = { isSearching = true },
                        onClearOrigin = {
                            originText = ""
                            explorer.setOriginQuery("")
                        },
                        onClearDestination = {
                            destText = ""
                            explorer.setDestinationQuery("")
                        },
                        onResetFilters = ::resetAll,
                        modifier = Modifier.padding(padding),
                    )
                    else -> SavedScreen(
                        savedFlights = saved,
                        selectedFlight = selected,
                        explorer = explorer,
                        modifier = Modifier.padding(padding),
                    )
                }
            }

            AnimatedVisibility(
                visible = isSearching,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
                modifier = Modifier.fillMaxSize(),
            ) {
                SearchScreen(
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
                    onClose = { isSearching = false },
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "searchCard"),
                            animatedVisibilityScope = this,
                            enter = EnterTransition.None,
                            exit = ExitTransition.None,
                            boundsTransform = { _, _ -> SearchSharedSpring },
                            // ScaleToBounds: lay out once at full size, then GPU-scale from the
                            // pill — content stays visible (no blank flash) and avoids per-frame
                            // remasure jank from RemeasureToBounds.
                            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(
                                contentScale = ContentScale.FillBounds,
                                alignment = Alignment.TopCenter,
                            ),
                            clipInOverlayDuringTransition = OverlayClip(RoameShapes.Panel),
                        ),
                )
            }
        }
    }
}

@Composable
private fun RoameBottomBar(
    tab: Int,
    onTab: (Int) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoameShapes.Pill, clip = false)
                .clip(RoameShapes.Pill)
                .background(RoameColors.Surface)
                .border(1.dp, RoameColors.Hairline, RoameShapes.Pill)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RoameTabItem(
                selected = tab == 0,
                label = "Discover",
                selectedIcon = Icons.Filled.Explore,
                unselectedIcon = Icons.Outlined.Explore,
                contentDescription = "Discover tab",
                onClick = { onTab(0) },
                modifier = Modifier.weight(1f),
            )
            RoameTabItem(
                selected = tab == 1,
                label = "Saved",
                selectedIcon = Icons.Filled.Favorite,
                unselectedIcon = Icons.Outlined.FavoriteBorder,
                contentDescription = "Saved tab",
                onClick = { onTab(1) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RoameTabItem(
    selected: Boolean,
    label: String,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) RoameColors.PinkSoft else Color.Transparent
    val fg = if (selected) RoameColors.Pink else RoameColors.Muted
    Row(
        modifier
            .clip(RoameShapes.Pill)
            .background(bg)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            label,
            color = fg,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
