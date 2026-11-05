package travel.roame.takehome

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
    val filteredGeneration: Long,
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
 * [FlightUiSnapshot] updates to a Swift callback whenever facade state changes.
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
        observeJob = explorer.watch(scope) { state ->
            onSnapshot(
                FlightUiSnapshot(
                    filters = state.filters,
                    filteredFlights = state.filteredFlights,
                    filteredGeneration = state.filteredGeneration,
                    clusters = state.clusters,
                    selectedFlight = state.selectedFlight,
                    savedFlights = state.savedFlights,
                    savedIds = state.savedIds,
                    status = state.status,
                    browsingCluster = state.browsingCluster,
                    saveError = state.saveError,
                ),
            )
        }
    }

    fun close() {
        observeJob?.cancel()
        explorer.close()
    }
}
