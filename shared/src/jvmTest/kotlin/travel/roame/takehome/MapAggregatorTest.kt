package travel.roame.takehome

import travel.roame.takehome.domain.MapAggregator
import travel.roame.takehome.domain.SearchFilterEngine
import travel.roame.takehome.model.FlightFilters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapAggregatorTest {
    private val flights = FixtureLoader.loadFlights()

    @Test
    fun groupsByDestinationWithCountAndMinPoints() {
        val clusters = MapAggregator.byDestination(flights)
        assertTrue(clusters.isNotEmpty())
        val byDest = flights.groupBy { it.destination }
        assertEquals(byDest.size, clusters.size)
        clusters.forEach { cluster ->
            val group = byDest.getValue(cluster.destination)
            assertEquals(group.size, cluster.flightCount)
            assertEquals(group.minOf { it.points }, cluster.lowestPoints)
            assertEquals(group.first().destinationCity, cluster.destinationCity)
        }
    }

    @Test
    fun aggregationReflectsFilteredSubset() {
        val filtered = SearchFilterEngine.apply(
            flights,
            FlightFilters(originQuery = "SEA", cabin = "Business"),
        )
        val clusters = MapAggregator.byDestination(filtered)
        assertTrue(clusters.isNotEmpty())
        assertEquals(filtered.map { it.destination }.toSet().size, clusters.size)
        assertEquals(filtered.size, clusters.sumOf { it.flightCount })
    }

    @Test
    fun mapClustersOmitNestedFlightsByDefault() {
        val clusters = MapAggregator.byDestination(flights)
        assertTrue(clusters.all { it.flights.isEmpty() })
    }

    @Test
    fun flightsForDestinationAreSortedByPointsThenDate() {
        val destination = flights.first().destination
        val sorted = MapAggregator.flightsForDestination(flights, destination)
        assertTrue(sorted.isNotEmpty())
        assertEquals(sorted, sorted.sortedWith(compareBy({ it.points }, { it.departureDate }, { it.id })))
    }
}
