package travel.roame.takehome.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import travel.roame.takehome.model.Flight
import travel.roame.takehome.model.LoadStatus

class FlightRepository(
    private val api: FlightsApi,
    private val store: FlightLocalStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    data class RefreshResult(
        val flights: List<Flight>,
        val status: LoadStatus,
    )

    private val dbMutex = Mutex()

    private suspend fun <T> db(block: () -> T): T =
        withContext(ioDispatcher) {
            dbMutex.withLock { block() }
        }

    suspend fun cachedFlights(): List<Flight> = db { store.readCache() }

    suspend fun savedFlights(): List<Flight> = db { store.readSaved() }

    suspend fun refresh(): RefreshResult {
        return try {
            val envelope = api.fetchFlights()
            db {
                store.replaceCache(envelope.flights)
                val flights = store.readCache()
                RefreshResult(
                    flights = flights,
                    status = if (flights.isEmpty()) LoadStatus.Empty else LoadStatus.Ready,
                )
            }
        } catch (t: Throwable) {
            val cache = db { store.readCache() }
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

    suspend fun saveFlight(flight: Flight): Result<List<Flight>> =
        try {
            Result.success(
                db {
                    store.upsertSaved(flight)
                    store.readSaved()
                },
            )
        } catch (t: Throwable) {
            Result.failure(t)
        }

    suspend fun removeSaved(id: String): Result<List<Flight>> =
        try {
            Result.success(
                db {
                    store.removeSaved(id)
                    store.readSaved()
                },
            )
        } catch (t: Throwable) {
            Result.failure(t)
        }
}
