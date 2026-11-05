package travel.roame.takehome.domain

import travel.roame.takehome.model.DestinationCluster
import travel.roame.takehome.model.Flight

object MapAggregator {
    private val flightOrder =
        compareBy<Flight> { it.points }.thenBy { it.departureDate }.thenBy { it.id }

    /**
     * Build destination clusters for the map.
     *
     * By default [DestinationCluster.flights] is left empty so filter recomputes stay cheap and
     * iOS bridge snapshots do not duplicate every flight under every marker. Hydrate flight lists
     * with [flightsForDestination] when a cluster sheet is opened.
     */
    fun byDestination(
        flights: List<Flight>,
        includeFlights: Boolean = false,
    ): List<DestinationCluster> =
        flights
            .groupBy { it.destination }
            .map { (destination, group) ->
                val first = group.first()
                DestinationCluster(
                    destination = destination,
                    destinationCity = first.destinationCity,
                    latitude = first.destinationLatitude,
                    longitude = first.destinationLongitude,
                    flightCount = group.size,
                    lowestPoints = group.minOf { it.points },
                    flights = if (includeFlights) {
                        group.sortedWith(flightOrder)
                    } else {
                        emptyList()
                    },
                )
            }
            .sortedBy { it.destination }

    fun flightsForDestination(flights: List<Flight>, destination: String): List<Flight> =
        flights
            .asSequence()
            .filter { it.destination == destination }
            .sortedWith(flightOrder)
            .toList()
}
