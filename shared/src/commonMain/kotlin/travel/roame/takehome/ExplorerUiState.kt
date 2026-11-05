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
            combine(filters, filteredFlights, clusters, selectedFlight, savedFlights) {
                    f, flights, c, selected, saved ->
                arrayOf(f, flights, c, selected, saved)
            },
            combine(savedIds, status, browsingCluster, saveError) { ids, st, browse, err ->
                arrayOf(ids, st, browse, err)
            },
        ) { left, right ->
            ExplorerUiState(
                filters = left[0] as FlightFilters,
                filteredFlights = left[1] as List<Flight>,
                clusters = left[2] as List<DestinationCluster>,
                selectedFlight = left[3] as Flight?,
                savedFlights = left[4] as List<Flight>,
                savedIds = right[0] as Set<String>,
                status = right[1] as LoadStatus,
                browsingCluster = right[2] as DestinationCluster?,
                saveError = right[3] as String?,
            )
        }.collect(onChange)
    }
}
