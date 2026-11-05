package travel.roame.takehome

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import travel.roame.takehome.data.FlightLocalStore
import travel.roame.takehome.data.FlightRepository
import travel.roame.takehome.data.FlightsApi
import travel.roame.takehome.db.FlightDatabase
import travel.roame.takehome.model.FlightFilters
import travel.roame.takehome.model.LoadStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FlightExplorerTest {
    private val fixtureJson = FixtureLoader::class.java.classLoader!!
        .getResourceAsStream("flights.json")!!
        .bufferedReader()
        .readText()

    private val fixtureFlights = FixtureLoader.loadFlights()

    private fun store(seed: Boolean = true): FlightLocalStore {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FlightDatabase.Schema.create(driver)
        return FlightLocalStore(driver).also { local ->
            if (seed) local.replaceCache(fixtureFlights)
        }
    }

    private fun api(engine: MockEngine): FlightsApi {
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return FlightsApi(baseUrl = "http://test", client = client)
    }

    private fun readyEngine(): MockEngine = MockEngine {
        respond(
            content = fixtureJson,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

    @Test
    fun bootstrapFromCacheReachesReady() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val job = SupervisorJob()
        val explorer = FlightExplorer(
            repository = FlightRepository(api(readyEngine()), store(seed = true), ioDispatcher = dispatcher),
            scope = CoroutineScope(job + dispatcher),
            computationDispatcher = dispatcher,
        )
        advanceUntilIdle()
        assertIs<LoadStatus.Ready>(explorer.status.value)
        assertEquals(1000, explorer.filteredFlights.value.size)
        explorer.close()
        job.cancel()
    }

    @Test
    fun originDebounceKeepsLatestQuery() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val job = SupervisorJob()
        val explorer = FlightExplorer(
            repository = FlightRepository(api(readyEngine()), store(seed = true), ioDispatcher = dispatcher),
            scope = CoroutineScope(job + dispatcher),
            computationDispatcher = dispatcher,
            searchDebounceMs = 250,
        )
        advanceUntilIdle()

        explorer.setOriginQuery("B")
        explorer.setOriginQuery("BO")
        explorer.setOriginQuery("BOS")
        assertEquals("", explorer.filters.value.originQuery)

        advanceTimeBy(249)
        assertEquals("", explorer.filters.value.originQuery)
        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals("BOS", explorer.filters.value.originQuery)
        assertTrue(explorer.filteredFlights.value.isNotEmpty())
        assertTrue(explorer.filteredFlights.value.all { it.origin == "BOS" })
        explorer.close()
        job.cancel()
    }

    @Test
    fun selectionClearsWhenFilteredOut() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val job = SupervisorJob()
        val explorer = FlightExplorer(
            repository = FlightRepository(api(readyEngine()), store(seed = true), ioDispatcher = dispatcher),
            scope = CoroutineScope(job + dispatcher),
            computationDispatcher = dispatcher,
            searchDebounceMs = 0,
        )
        advanceUntilIdle()

        val bos = explorer.filteredFlights.value.first { it.origin == "BOS" }
        explorer.selectFlight(bos.id)
        assertEquals(bos.id, explorer.selectedFlightId.value)
        assertNotNull(explorer.selectedFlight.value)

        explorer.setOriginQuery("ZZZ_NO_MATCH")
        advanceUntilIdle()
        assertNull(explorer.selectedFlightId.value)
        assertNull(explorer.selectedFlight.value)
        explorer.close()
        job.cancel()
    }

    @Test
    fun resetFiltersRestoresDefaultsImmediately() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val job = SupervisorJob()
        val explorer = FlightExplorer(
            repository = FlightRepository(api(readyEngine()), store(seed = true), ioDispatcher = dispatcher),
            scope = CoroutineScope(job + dispatcher),
            computationDispatcher = dispatcher,
            searchDebounceMs = 250,
        )
        advanceUntilIdle()

        explorer.setCabin("Business")
        advanceUntilIdle()
        assertEquals("Business", explorer.filters.value.cabin)

        explorer.setOriginQuery("pending")
        explorer.resetFilters()
        advanceUntilIdle()

        assertEquals(FlightFilters.defaults(), explorer.filters.value)
        assertEquals(1000, explorer.filteredFlights.value.size)
        explorer.close()
        job.cancel()
    }

    @Test
    fun offlineRefreshSurfacesStaleStatus() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val job = SupervisorJob()
        val engine = MockEngine {
            respond("down", status = HttpStatusCode.ServiceUnavailable)
        }
        val explorer = FlightExplorer(
            repository = FlightRepository(api(engine), store(seed = true), ioDispatcher = dispatcher),
            scope = CoroutineScope(job + dispatcher),
            computationDispatcher = dispatcher,
        )
        advanceUntilIdle()
        assertIs<LoadStatus.Ready>(explorer.status.value)
        assertEquals(1000, explorer.allFlights.value.size)

        explorer.refresh()
        // Ktor resumes onto the test dispatcher from another thread — pump until stale.
        repeat(40) {
            advanceUntilIdle()
            if (explorer.status.value is LoadStatus.OfflineStale) return@repeat
            Thread.sleep(25)
        }
        assertIs<LoadStatus.OfflineStale>(explorer.status.value)
        assertEquals(1000, explorer.allFlights.value.size)
        explorer.close()
        job.cancel()
    }

    @Test
    fun saveAndRemoveUpdateSavedIds() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val job = SupervisorJob()
        val explorer = FlightExplorer(
            repository = FlightRepository(api(readyEngine()), store(seed = true), ioDispatcher = dispatcher),
            scope = CoroutineScope(job + dispatcher),
            computationDispatcher = dispatcher,
        )
        advanceUntilIdle()
        val flight = explorer.filteredFlights.value.first()
        explorer.saveFlight(flight)
        advanceUntilIdle()
        assertTrue(flight.id in explorer.savedIds.value)
        assertNull(explorer.saveError.value)

        explorer.removeSaved(flight.id)
        advanceUntilIdle()
        assertTrue(flight.id !in explorer.savedIds.value)
        explorer.close()
        job.cancel()
    }
}
