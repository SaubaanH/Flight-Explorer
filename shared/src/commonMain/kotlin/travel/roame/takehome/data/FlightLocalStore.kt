package travel.roame.takehome.data

import app.cash.sqldelight.db.SqlDriver
import travel.roame.takehome.db.FlightDatabase
import travel.roame.takehome.db.Flight_cache
import travel.roame.takehome.db.Saved_flights
import travel.roame.takehome.model.Flight

class FlightLocalStore(driver: SqlDriver) {
    private val db = FlightDatabase(driver)

    fun readCache(): List<Flight> =
        db.flightDatabaseQueries.selectAllCache().executeAsList().map { it.toFlight() }

    fun replaceCache(flights: List<Flight>) {
        db.flightDatabaseQueries.transaction {
            db.flightDatabaseQueries.deleteAllCache()
            flights.forEach { flight ->
                db.flightDatabaseQueries.insertCache(
                    id = flight.id,
                    origin = flight.origin,
                    destination = flight.destination,
                    departureDate = flight.departureDate,
                    program = flight.program,
                    points = flight.points.toLong(),
                    cabin = flight.cabin,
                    originCity = flight.originCity,
                    destinationCity = flight.destinationCity,
                    originLatitude = flight.originLatitude,
                    originLongitude = flight.originLongitude,
                    destinationLatitude = flight.destinationLatitude,
                    destinationLongitude = flight.destinationLongitude,
                )
            }
        }
    }

    fun readSaved(): List<Flight> =
        db.flightDatabaseQueries.selectAllSaved().executeAsList().map { it.toFlight() }

    fun isSaved(id: String): Boolean =
        db.flightDatabaseQueries.selectSavedById(id).executeAsOneOrNull() != null

    fun upsertSaved(flight: Flight, savedAt: Long = currentTimeMillis()) {
        db.flightDatabaseQueries.upsertSaved(
            id = flight.id,
            origin = flight.origin,
            destination = flight.destination,
            departureDate = flight.departureDate,
            program = flight.program,
            points = flight.points.toLong(),
            cabin = flight.cabin,
            originCity = flight.originCity,
            destinationCity = flight.destinationCity,
            originLatitude = flight.originLatitude,
            originLongitude = flight.originLongitude,
            destinationLatitude = flight.destinationLatitude,
            destinationLongitude = flight.destinationLongitude,
            savedAt = savedAt,
        )
    }

    fun removeSaved(id: String) {
        db.flightDatabaseQueries.deleteSaved(id)
    }
}

internal expect fun currentTimeMillis(): Long

private fun Flight_cache.toFlight(): Flight = Flight(
    id = id,
    origin = origin,
    destination = destination,
    departureDate = departureDate,
    program = program,
    points = points.toInt(),
    cabin = cabin,
    originCity = originCity,
    destinationCity = destinationCity,
    originLatitude = originLatitude,
    originLongitude = originLongitude,
    destinationLatitude = destinationLatitude,
    destinationLongitude = destinationLongitude,
)

private fun Saved_flights.toFlight(): Flight = Flight(
    id = id,
    origin = origin,
    destination = destination,
    departureDate = departureDate,
    program = program,
    points = points.toInt(),
    cabin = cabin,
    originCity = originCity,
    destinationCity = destinationCity,
    originLatitude = originLatitude,
    originLongitude = originLongitude,
    destinationLatitude = destinationLatitude,
    destinationLongitude = destinationLongitude,
)
