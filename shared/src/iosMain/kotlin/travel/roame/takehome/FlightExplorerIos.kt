package travel.roame.takehome

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
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
 *
 * Emits are coalesced so a single filter recompute (which updates multiple flows)
 * becomes one SwiftUI publish instead of several full-list diffs.
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
            val ticks = MutableSharedFlow<Unit>(
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

            fun requestEmit() {
                ticks.tryEmit(Unit)
            }

            fun snapshot(): FlightUiSnapshot =
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
                )

            launch {
                ticks.collectLatest {
                    // Coalesce same-frame / burst updates from multiple StateFlows.
                    delay(SNAPSHOT_COALESCE_MS)
                    onSnapshot(snapshot())
                }
            }

            requestEmit()
            launch { explorer.filters.collect { requestEmit() } }
            launch { explorer.filteredFlights.collect { requestEmit() } }
            launch { explorer.clusters.collect { requestEmit() } }
            launch { explorer.selectedFlight.collect { requestEmit() } }
            launch { explorer.savedFlights.collect { requestEmit() } }
            launch { explorer.savedIds.collect { requestEmit() } }
            launch { explorer.status.collect { requestEmit() } }
            launch { explorer.browsingCluster.collect { requestEmit() } }
            launch { explorer.saveError.collect { requestEmit() } }
        }
    }

    fun close() {
        observeJob?.cancel()
        explorer.close()
    }

    companion object {
        private const val SNAPSHOT_COALESCE_MS: Long = 16L
    }
}
