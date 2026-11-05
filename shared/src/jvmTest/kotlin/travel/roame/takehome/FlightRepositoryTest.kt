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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import travel.roame.takehome.data.FlightLocalStore
import travel.roame.takehome.data.FlightRepository
import travel.roame.takehome.data.FlightsApi
import travel.roame.takehome.db.FlightDatabase
import travel.roame.takehome.model.LoadStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FlightRepositoryTest {
    private val fixtureJson = FixtureLoader::class.java.classLoader!!
        .getResourceAsStream("flights.json")!!
        .bufferedReader()
        .readText()

    private fun store(): FlightLocalStore {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FlightDatabase.Schema.create(driver)
        return FlightLocalStore(driver)
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

    @Test
    fun successfulRefreshReplacesCache() = runTest {
        val engine = MockEngine {
            respond(
                content = fixtureJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repo = FlightRepository(api(engine), store())
        val result = repo.refresh()
        assertEquals(1000, result.flights.size)
        assertIs<LoadStatus.Ready>(result.status)
        assertEquals(1000, repo.cachedFlights().size)
    }

    @Test
    fun failedRefreshPreservesCacheAsOfflineStale() = runTest {
        var calls = 0
        val engine = MockEngine {
            calls++
            if (calls == 1) {
                respond(
                    content = fixtureJson,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            } else {
                respond("down", status = HttpStatusCode.ServiceUnavailable)
            }
        }
        val repo = FlightRepository(api(engine), store())
        repo.refresh()
        val stale = repo.refresh()
        assertEquals(1000, stale.flights.size)
        assertIs<LoadStatus.OfflineStale>(stale.status)
    }

    @Test
    fun failedRefreshWithoutCacheIsError() = runTest {
        val engine = MockEngine {
            respond("nope", status = HttpStatusCode.InternalServerError)
        }
        val repo = FlightRepository(api(engine), store())
        val result = repo.refresh()
        assertTrue(result.flights.isEmpty())
        assertIs<LoadStatus.Error>(result.status)
    }

    @Test
    fun saveUpsertsWithoutDuplicatesAndRemoveWorks() = runTest {
        val local = store()
        val repo = FlightRepository(
            api(
                MockEngine {
                    respond("{}", status = HttpStatusCode.OK)
                },
            ),
            local,
        )
        val flight = FixtureLoader.loadFlights().first()
        val updated = flight.copy(points = flight.points + 1000)
        assertTrue(repo.saveFlight(flight).isSuccess)
        assertTrue(repo.saveFlight(updated).isSuccess)
        val saved = repo.savedFlights()
        assertEquals(1, saved.size)
        assertEquals(updated.points, saved.single().points)
        assertTrue(repo.removeSaved(flight.id).isSuccess)
        assertTrue(repo.savedFlights().isEmpty())
    }

    @Test
    fun savedSurvivesCacheReplacementAbsence() = runTest {
        val local = store()
        val flight = FixtureLoader.loadFlights().first()
        local.upsertSaved(flight)
        val engine = MockEngine {
            respond(
                content = """{"flights":[]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repo = FlightRepository(api(engine), local)
        val result = repo.refresh()
        assertIs<LoadStatus.Empty>(result.status)
        assertEquals(1, repo.savedFlights().size)
        assertEquals(flight.id, repo.savedFlights().single().id)
    }
}
