package travel.roame.takehome

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import travel.roame.takehome.data.DatabaseDriverFactory
import travel.roame.takehome.model.DestinationCluster
import travel.roame.takehome.model.Flight
import travel.roame.takehome.model.FlightFilters
import travel.roame.takehome.model.LoadStatus

/**
 * Snapshot of facade state for SwiftUI. Collected on the main dispatcher.
 */
data class FlightUiSnapshot(
    val filters: FlightFilters,
    val filteredFlights: List<Flight>,
    val clusters: List<DestinationCluster>,
    val selectedFlight: Flight?,
    val savedFlights: List<Flight>,
    val savedIds: Set<String>,
    val status: LoadStatus,
    val browsingCluster: DestinationCluster?,
    val saveError: String?,
)

/**
 * iOS-facing host: creates [FlightExplorer] for the simulator base URL and pushes
 * [FlightUiSnapshot] updates to a Swift callback whenever any StateFlow changes.
 */
class FlightExplorerIos(
    baseUrl: String = ApiBaseUrls.IOS_SIMULATOR,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val explorer: FlightExplorer = FlightExplorerFactory.create(
        baseUrl = baseUrl,
        driverFactory = DatabaseDriverFactory(),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )

    private var observeJob: Job? = null

    fun start(onSnapshot: (FlightUiSnapshot) -> Unit) {
        observeJob?.cancel()
        observeJob = scope.launch {
            fun emit() {
                onSnapshot(
                    FlightUiSnapshot(
                        filters = explorer.filters.value,
                        filteredFlights = explorer.filteredFlights.value,
                        clusters = explorer.clusters.value,
                        selectedFlight = explorer.selectedFlight.value,
                        savedFlights = explorer.savedFlights.value,
                        savedIds = explorer.savedIds.value,
                        status = explorer.status.value,
                        browsingCluster = explorer.browsingCluster.value,
                        saveError = explorer.saveError.value,
                    ),
                )
            }
            emit()
            launch { explorer.filters.collect { emit() } }
            launch { explorer.filteredFlights.collect { emit() } }
            launch { explorer.clusters.collect { emit() } }
            launch { explorer.selectedFlight.collect { emit() } }
            launch { explorer.savedFlights.collect { emit() } }
            launch { explorer.savedIds.collect { emit() } }
            launch { explorer.status.collect { emit() } }
            launch { explorer.browsingCluster.collect { emit() } }
            launch { explorer.saveError.collect { emit() } }
        }
    }

    fun close() {
        observeJob?.cancel()
        explorer.close()
    }
}
