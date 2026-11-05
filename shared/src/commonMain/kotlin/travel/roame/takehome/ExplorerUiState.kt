package travel.roame.takehome

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import travel.roame.takehome.model.DestinationCluster
import travel.roame.takehome.model.Flight
import travel.roame.takehome.model.FlightFilters
import travel.roame.takehome.model.LoadStatus

data class ExplorerUiState(
    val filters: FlightFilters = FlightFilters.defaults(),
    val filteredFlights: List<Flight> = emptyList(),
    val filteredGeneration: Long = 0L,
    val clusters: List<DestinationCluster> = emptyList(),
    val selectedFlight: Flight? = null,
    val savedFlights: List<Flight> = emptyList(),
    val savedIds: Set<String> = emptySet(),
    val status: LoadStatus = LoadStatus.Loading,
    val browsingCluster: DestinationCluster? = null,
    val saveError: String? = null,
)

fun FlightExplorer.snapshot(): ExplorerUiState = ExplorerUiState(
    filters = filters.value,
    filteredFlights = filteredFlights.value,
    filteredGeneration = filteredGeneration.value,
    clusters = clusters.value,
    selectedFlight = selectedFlight.value,
    savedFlights = savedFlights.value,
    savedIds = savedIds.value,
    status = status.value,
    browsingCluster = browsingCluster.value,
    saveError = saveError.value,
)

/**
 * Collects facade StateFlows into a single snapshot for native UI bridges.
 * Invokes [onChange] on the collector context — hop to main in Swift if needed.
 */
fun FlightExplorer.watch(
    scope: CoroutineScope,
    onChange: (ExplorerUiState) -> Unit,
): Job {
    onChange(snapshot())
    return scope.launch {
        combine(
            combine(filters, filteredFlights, filteredGeneration, clusters, selectedFlight) {
                    f, flights, generation, c, selected ->
                arrayOf(f, flights, generation, c, selected)
            },
            combine(savedFlights, savedIds, status, browsingCluster, saveError) {
                    saved, ids, st, browse, err ->
                arrayOf(saved, ids, st, browse, err)
            },
        ) { left, right ->
            ExplorerUiState(
                filters = left[0] as FlightFilters,
                filteredFlights = left[1] as List<Flight>,
                filteredGeneration = left[2] as Long,
                clusters = left[3] as List<DestinationCluster>,
                selectedFlight = left[4] as Flight?,
                savedFlights = right[0] as List<Flight>,
                savedIds = right[1] as Set<String>,
                status = right[2] as LoadStatus,
                browsingCluster = right[3] as DestinationCluster?,
                saveError = right[4] as String?,
            )
        }.collect(onChange)
    }
}
