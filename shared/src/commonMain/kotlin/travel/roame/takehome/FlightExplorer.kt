package travel.roame.takehome

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import travel.roame.takehome.data.FlightRepository
import travel.roame.takehome.domain.MapAggregator
import travel.roame.takehome.domain.SearchFilterEngine
import travel.roame.takehome.model.DestinationCluster
import travel.roame.takehome.model.Flight
import travel.roame.takehome.model.FlightFilters
import travel.roame.takehome.model.LoadStatus

class FlightExplorer(
    private val repository: FlightRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val searchDebounceMs: Long = SEARCH_DEBOUNCE_MS,
) {
    private val _filters = MutableStateFlow(FlightFilters.defaults())
    val filters: StateFlow<FlightFilters> = _filters.asStateFlow()

    private val _allFlights = MutableStateFlow<List<Flight>>(emptyList())
    val allFlights: StateFlow<List<Flight>> = _allFlights.asStateFlow()

    private val _filteredFlights = MutableStateFlow<List<Flight>>(emptyList())
    val filteredFlights: StateFlow<List<Flight>> = _filteredFlights.asStateFlow()

    /** Bumps only when [filteredFlights] is recomputed — iOS uses this to skip re-bridging. */
    private val _filteredGeneration = MutableStateFlow(0L)
    val filteredGeneration: StateFlow<Long> = _filteredGeneration.asStateFlow()

    private val _clusters = MutableStateFlow<List<DestinationCluster>>(emptyList())
    val clusters: StateFlow<List<DestinationCluster>> = _clusters.asStateFlow()

    private val _selectedFlightId = MutableStateFlow<String?>(null)
    val selectedFlightId: StateFlow<String?> = _selectedFlightId.asStateFlow()

    private val _selectedFlight = MutableStateFlow<Flight?>(null)
    val selectedFlight: StateFlow<Flight?> = _selectedFlight.asStateFlow()

    private val _savedFlights = MutableStateFlow<List<Flight>>(emptyList())
    val savedFlights: StateFlow<List<Flight>> = _savedFlights.asStateFlow()

    private val _savedIds = MutableStateFlow<Set<String>>(emptySet())
    val savedIds: StateFlow<Set<String>> = _savedIds.asStateFlow()

    private val _status = MutableStateFlow<LoadStatus>(LoadStatus.Loading)
    val status: StateFlow<LoadStatus> = _status.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    private val _browsingCluster = MutableStateFlow<DestinationCluster?>(null)
    val browsingCluster: StateFlow<DestinationCluster?> = _browsingCluster.asStateFlow()

    private var filterJob: Job? = null
    private var originDebounceJob: Job? = null
    private var destinationDebounceJob: Job? = null
    private var refreshJob: Job? = null

    init {
        // Bootstrap cache/saved off the caller thread, then refresh the network.
        scope.launch {
            val cached = repository.cachedFlights()
            val saved = repository.savedFlights()
            _allFlights.value = cached
            _savedFlights.value = saved
            _savedIds.value = saved.map { it.id }.toSet()
            if (cached.isNotEmpty()) {
                _status.value = LoadStatus.Ready
            }
            recomputeFilters()
            refresh()
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val hadCache = _allFlights.value.isNotEmpty()
            if (!hadCache) {
                _status.value = LoadStatus.Loading
            }
            val result = repository.refresh()
            _allFlights.value = result.flights
            _status.value = result.status
            recomputeFilters()
        }
    }

    fun setOriginQuery(raw: String) {
        originDebounceJob?.cancel()
        originDebounceJob = scope.launch {
            delay(searchDebounceMs)
            updateFilters { it.copy(originQuery = raw) }
        }
    }

    fun setDestinationQuery(raw: String) {
        destinationDebounceJob?.cancel()
        destinationDebounceJob = scope.launch {
            delay(searchDebounceMs)
            updateFilters { it.copy(destinationQuery = raw) }
        }
    }

    fun setDateRange(from: String, to: String) {
        updateFilters { it.copy(dateFrom = from, dateTo = to) }
    }

    fun setCabin(cabin: String?) {
        updateFilters { it.copy(cabin = cabin) }
    }

    fun setMaxPoints(maxPoints: Int?) {
        updateFilters { it.copy(maxPoints = maxPoints) }
    }

    /** Swift-friendly helpers (Kotlin Int? bridging is awkward from Swift). */
    fun setMaxPointsValue(maxPoints: Int) = setMaxPoints(maxPoints)

    fun clearMaxPoints() = setMaxPoints(null)

    fun toggleProgram(program: String) {
        updateFilters { current ->
            val next = current.programs.toMutableSet()
            if (!next.add(program)) next.remove(program)
            current.copy(programs = next)
        }
    }

    fun resetFilters() {
        originDebounceJob?.cancel()
        destinationDebounceJob?.cancel()
        _filters.value = FlightFilters.defaults()
        recomputeFiltersImmediate()
    }

    fun selectFlight(id: String?) {
        _selectedFlightId.value = id
        syncSelectedFlight()
        if (id == null) return
        val flight = _filteredFlights.value.firstOrNull { it.id == id }
            ?: _allFlights.value.firstOrNull { it.id == id }
            ?: _savedFlights.value.firstOrNull { it.id == id }
        flight?.let { _browsingCluster.value = null }
    }

    fun openCluster(cluster: DestinationCluster) {
        _browsingCluster.value = hydrateCluster(cluster, _filteredFlights.value)
    }

    fun dismissCluster() {
        _browsingCluster.value = null
    }

    fun saveSelected() {
        val flight = _selectedFlight.value ?: return
        saveFlight(flight)
    }

    /** Persists on a background dispatcher; outcome arrives via [savedFlights] / [saveError]. */
    fun saveFlight(flight: Flight) {
        scope.launch {
            val result = repository.saveFlight(flight)
            result.fold(
                onSuccess = { saved ->
                    _savedFlights.value = saved
                    _savedIds.value = saved.map { it.id }.toSet()
                    _saveError.value = null
                },
                onFailure = { error ->
                    _saveError.value = error.message ?: "Failed to save flight"
                },
            )
        }
    }

    fun removeSaved(id: String) {
        scope.launch {
            val result = repository.removeSaved(id)
            result.fold(
                onSuccess = { saved ->
                    _savedFlights.value = saved
                    _savedIds.value = saved.map { it.id }.toSet()
                    _saveError.value = null
                },
                onFailure = { error ->
                    _saveError.value = error.message ?: "Failed to remove saved flight"
                },
            )
        }
    }

    fun clearSaveError() {
        _saveError.value = null
    }

    fun close() {
        filterJob?.cancel()
        originDebounceJob?.cancel()
        destinationDebounceJob?.cancel()
        refreshJob?.cancel()
        scope.cancel()
    }

    private fun updateFilters(transform: (FlightFilters) -> FlightFilters) {
        _filters.update(transform)
        scheduleFilterRecompute()
    }

    private fun scheduleFilterRecompute() {
        filterJob?.cancel()
        filterJob = scope.launch {
            recomputeFilters()
        }
    }

    private fun recomputeFiltersImmediate() {
        filterJob?.cancel()
        filterJob = scope.launch {
            recomputeFilters()
        }
    }

    private suspend fun recomputeFilters() {
        val filters = _filters.value
        val source = _allFlights.value
        val filtered = withContext(computationDispatcher) {
            SearchFilterEngine.apply(source, filters)
        }
        val clusters = withContext(computationDispatcher) {
            MapAggregator.byDestination(filtered)
        }
        _filteredFlights.value = filtered
        _filteredGeneration.update { it + 1 }
        _clusters.value = clusters
        val kept = SearchFilterEngine.clearSelectionIfFilteredOut(_selectedFlightId.value, filtered)
        if (kept != _selectedFlightId.value) {
            _selectedFlightId.value = kept
        }
        syncSelectedFlight()
        val browsing = _browsingCluster.value
        if (browsing != null) {
            val refreshed = clusters.firstOrNull { it.destination == browsing.destination }
            _browsingCluster.value = refreshed?.let { hydrateCluster(it, filtered) }
        }
    }

    private fun syncSelectedFlight() {
        val id = _selectedFlightId.value
        _selectedFlight.value = when {
            id == null -> null
            else ->
                _filteredFlights.value.firstOrNull { it.id == id }
                    ?: _savedFlights.value.firstOrNull { it.id == id }
                    ?: _allFlights.value.firstOrNull { it.id == id }
        }
    }

    private fun hydrateCluster(
        cluster: DestinationCluster,
        filtered: List<Flight>,
    ): DestinationCluster =
        cluster.copy(flights = MapAggregator.flightsForDestination(filtered, cluster.destination))

    companion object {
        const val SEARCH_DEBOUNCE_MS: Long = 250L
        /** Initial / incremental page size for Discover list UIs. */
        const val LIST_PAGE_SIZE: Int = 24
    }
}
