package travel.roame.takehome.android.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import travel.roame.takehome.FlightExplorer

@Composable
fun FlightExplorerApp(explorer: FlightExplorer) {
    var tab by remember { mutableIntStateOf(0) }
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

    LaunchedEffect(saveError) {
        val message = saveError ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        explorer.clearSaveError()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Discover tab") },
                    label = { Text("Discover") },
                    modifier = Modifier.semantics { contentDescription = "Discover tab" },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Saved tab") },
                    label = { Text("Saved") },
                    modifier = Modifier.semantics { contentDescription = "Saved tab" },
                )
            }
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
}
