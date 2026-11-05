package travel.roame.takehome

import kotlinx.serialization.json.Json
import travel.roame.takehome.model.Flight
import travel.roame.takehome.model.FlightsEnvelope

object FixtureLoader {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadFlights(): List<Flight> {
        val text = readResource("flights.json")
        return json.decodeFromString(FlightsEnvelope.serializer(), text).flights
    }

    private fun readResource(name: String): String {
        val stream = FixtureLoader::class.java.classLoader?.getResourceAsStream(name)
            ?: error("Missing test resource $name")
        return stream.bufferedReader().use { it.readText() }
    }
}
