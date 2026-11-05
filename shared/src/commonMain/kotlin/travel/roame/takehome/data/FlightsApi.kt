package travel.roame.takehome.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import travel.roame.takehome.model.FlightsEnvelope

class FlightsApi(
    private val baseUrl: String,
    private val client: HttpClient = defaultClient(),
) {
    suspend fun fetchFlights(): FlightsEnvelope {
        val root = baseUrl.trimEnd('/')
        return client.get("$root/flights").body()
    }

    fun close() {
        client.close()
    }

    companion object {
        fun defaultClient(): HttpClient = HttpClient {
            expectSuccess = true
            install(HttpTimeout) {
                connectTimeoutMillis = 30_000
                requestTimeoutMillis = 60_000
                socketTimeoutMillis = 60_000
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
        }
    }
}
