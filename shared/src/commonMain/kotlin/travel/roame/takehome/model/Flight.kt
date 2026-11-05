package travel.roame.takehome.model

import kotlinx.serialization.Serializable

@Serializable
data class FlightsEnvelope(
    val flights: List<Flight>,
)

@Serializable
data class Flight(
    val id: String,
    val origin: String,
    val destination: String,
    val departureDate: String,
    val program: String,
    val points: Int,
    val cabin: String,
    val originCity: String,
    val destinationCity: String,
    val originLatitude: Double,
    val originLongitude: Double,
    val destinationLatitude: Double,
    val destinationLongitude: Double,
)

data class DestinationCluster(
    val destination: String,
    val destinationCity: String,
    val latitude: Double,
    val longitude: Double,
    val flightCount: Int,
    val lowestPoints: Int,
    val flights: List<Flight>,
)

enum class Cabin(val label: String) {
    Economy("Economy"),
    PremiumEconomy("Premium Economy"),
    Business("Business"),
    ;

    companion object {
        val allLabels: List<String> = entries.map { it.label }

        fun fromLabel(label: String): Cabin? =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) }
    }
}

object LoyaltyPrograms {
    val all: List<String> = listOf(
        "Aeroplan",
        "Avios",
        "Flying Blue",
        "Mileage Plan",
        "MileagePlus",
    )
}

data class FlightFilters(
    val originQuery: String = "",
    val destinationQuery: String = "",
    val dateFrom: String = DEFAULT_DATE_FROM,
    val dateTo: String = DEFAULT_DATE_TO,
    val cabin: String? = null,
    val maxPoints: Int? = null,
    val programs: Set<String> = emptySet(),
) {
    companion object {
        const val DEFAULT_DATE_FROM = "2027-02-01"
        const val DEFAULT_DATE_TO = "2027-04-01"

        fun defaults(): FlightFilters = FlightFilters()
    }

    fun isDefault(): Boolean =
        originQuery.isBlank() &&
            destinationQuery.isBlank() &&
            dateFrom == DEFAULT_DATE_FROM &&
            dateTo == DEFAULT_DATE_TO &&
            cabin == null &&
            maxPoints == null &&
            programs.isEmpty()
}

sealed class LoadStatus {
    data object Loading : LoadStatus()
    data object Ready : LoadStatus()
    data object Empty : LoadStatus()
    data object OfflineStale : LoadStatus()
    data class Error(val message: String) : LoadStatus()
}
