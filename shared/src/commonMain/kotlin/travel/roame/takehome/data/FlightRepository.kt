package travel.roame.takehome.data

import travel.roame.takehome.model.Flight
import travel.roame.takehome.model.LoadStatus

class FlightRepository(
    private val api: FlightsApi,
    private val store: FlightLocalStore,
) {
    data class RefreshResult(
        val flights: List<Flight>,
        val status: LoadStatus,
    )

    fun cachedFlights(): List<Flight> = store.readCache()

    fun savedFlights(): List<Flight> = store.readSaved()

    fun savedIds(): Set<String> = store.readSaved().map { it.id }.toSet()

    fun bootstrapStatus(): LoadStatus {
        val cache = store.readCache()
        return when {
            cache.isEmpty() -> LoadStatus.Loading
            else -> LoadStatus.Ready
        }
    }

    suspend fun refresh(): RefreshResult {
        return try {
            val envelope = api.fetchFlights()
            store.replaceCache(envelope.flights)
            val flights = store.readCache()
            RefreshResult(
                flights = flights,
                status = if (flights.isEmpty()) LoadStatus.Empty else LoadStatus.Ready,
            )
        } catch (t: Throwable) {
            val cache = store.readCache()
            if (cache.isNotEmpty()) {
                RefreshResult(flights = cache, status = LoadStatus.OfflineStale)
            } else {
                RefreshResult(
                    flights = emptyList(),
                    status = LoadStatus.Error(t.message ?: "Failed to load flights"),
                )
            }
        }
    }

    fun saveFlight(flight: Flight): Result<List<Flight>> =
        runCatching {
            store.upsertSaved(flight)
            store.readSaved()
        }

    fun removeSaved(id: String): Result<List<Flight>> =
        runCatching {
            store.removeSaved(id)
            store.readSaved()
        }
}
