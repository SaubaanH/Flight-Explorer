package travel.roame.takehome.domain

import travel.roame.takehome.model.Flight
import travel.roame.takehome.model.FlightFilters

object SearchFilterEngine {
    fun apply(flights: List<Flight>, filters: FlightFilters): List<Flight> =
        flights.filter { matches(it, filters) }

    fun matches(flight: Flight, filters: FlightFilters): Boolean {
        if (!matchesAirport(filters.originQuery, flight.origin, flight.originCity)) return false
        if (!matchesAirport(filters.destinationQuery, flight.destination, flight.destinationCity)) {
            return false
        }
        if (flight.departureDate < filters.dateFrom || flight.departureDate > filters.dateTo) {
            return false
        }
        filters.cabin?.takeIf { it.isNotBlank() }?.let { cabin ->
            if (!flight.cabin.equals(cabin, ignoreCase = true)) return false
        }
        filters.maxPoints?.let { max ->
            if (flight.points > max) return false
        }
        if (filters.programs.isNotEmpty()) {
            val wanted = filters.programs.map { it.trim().lowercase() }.toSet()
            if (flight.program.trim().lowercase() !in wanted) return false
        }
        return true
    }

    /** Airport code or city name; trim + case-insensitive; blank query matches all. */
    fun matchesAirport(query: String, code: String, city: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        val needle = q.lowercase()
        return code.lowercase().contains(needle) || city.lowercase().contains(needle)
    }

    fun clearSelectionIfFilteredOut(selectedId: String?, filtered: List<Flight>): String? {
        if (selectedId == null) return null
        return if (filtered.any { it.id == selectedId }) selectedId else null
    }
}
