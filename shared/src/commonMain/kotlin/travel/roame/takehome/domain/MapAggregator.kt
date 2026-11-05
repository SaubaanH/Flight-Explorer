package travel.roame.takehome.domain

import travel.roame.takehome.model.DestinationCluster
import travel.roame.takehome.model.Flight

object MapAggregator {
    fun byDestination(flights: List<Flight>): List<DestinationCluster> =
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
                    flights = group.sortedWith(
                        compareBy<Flight> { it.points }.thenBy { it.departureDate }.thenBy { it.id },
                    ),
                )
            }
            .sortedBy { it.destination }
}
