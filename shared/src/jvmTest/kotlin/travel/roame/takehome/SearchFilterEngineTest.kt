package travel.roame.takehome

import travel.roame.takehome.domain.SearchFilterEngine
import travel.roame.takehome.model.FlightFilters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchFilterEngineTest {
    private val flights = FixtureLoader.loadFlights()

    @Test
    fun emptyFiltersReturnAllFlights() {
        val result = SearchFilterEngine.apply(flights, FlightFilters.defaults())
        assertEquals(1000, result.size)
    }

    @Test
    fun originMatchesIataCaseAndTrimInsensitive() {
        val filters = FlightFilters(originQuery = "  bos ")
        val result = SearchFilterEngine.apply(flights, filters)
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.origin == "BOS" })
    }

    @Test
    fun originMatchesCityName() {
        val filters = FlightFilters(originQuery = "boston")
        val result = SearchFilterEngine.apply(flights, filters)
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.originCity.equals("Boston", ignoreCase = true) })
    }

    @Test
    fun destinationMatchesCityOrCode() {
        val byCode = SearchFilterEngine.apply(flights, FlightFilters(destinationQuery = "lhr"))
        val byCity = SearchFilterEngine.apply(flights, FlightFilters(destinationQuery = "London"))
        assertTrue(byCode.isNotEmpty())
        assertTrue(byCity.isNotEmpty())
        assertTrue(byCode.all { it.destination == "LHR" })
    }

    @Test
    fun dateRangeIsInclusiveOnBothBounds() {
        val filters = FlightFilters(dateFrom = "2027-02-01", dateTo = "2027-02-01")
        val result = SearchFilterEngine.apply(flights, filters)
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.departureDate == "2027-02-01" })

        val end = SearchFilterEngine.apply(
            flights,
            FlightFilters(dateFrom = "2027-04-01", dateTo = "2027-04-01"),
        )
        assertTrue(end.isNotEmpty())
        assertTrue(end.all { it.departureDate == "2027-04-01" })
    }

    @Test
    fun maxPointsIsInclusive() {
        val limit = 25000
        val result = SearchFilterEngine.apply(flights, FlightFilters(maxPoints = limit))
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.points <= limit })
        assertTrue(result.any { it.points == limit })
    }

    @Test
    fun cabinFilterIsExactDimensionAnd() {
        val result = SearchFilterEngine.apply(
            flights,
            FlightFilters(cabin = "Business", originQuery = "JFK"),
        )
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.cabin == "Business" && it.origin == "JFK" })
    }

    @Test
    fun programsUseOrWithinAndAcrossDimensions() {
        val filters = FlightFilters(
            programs = setOf("Avios", "Aeroplan"),
            cabin = "Economy",
        )
        val result = SearchFilterEngine.apply(flights, filters)
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.cabin == "Economy" })
        assertTrue(result.all { it.program == "Avios" || it.program == "Aeroplan" })
        assertTrue(result.any { it.program == "Avios" })
        assertTrue(result.any { it.program == "Aeroplan" })
    }

    @Test
    fun clearSelectionWhenFilteredOut() {
        val id = flights.first().id
        val filtered = SearchFilterEngine.apply(
            flights,
            FlightFilters(originQuery = "ZZZ"),
        )
        assertEquals(emptyList(), filtered)
        assertEquals(null, SearchFilterEngine.clearSelectionIfFilteredOut(id, filtered))
        assertEquals(
            id,
            SearchFilterEngine.clearSelectionIfFilteredOut(id, flights.take(5)),
        )
    }
}
