package travel.roame.takehome

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import travel.roame.takehome.data.DatabaseDriverFactory
import travel.roame.takehome.data.FlightLocalStore
import travel.roame.takehome.data.FlightRepository
import travel.roame.takehome.data.FlightsApi

object FlightExplorerFactory {
    fun create(
        baseUrl: String,
        driverFactory: DatabaseDriverFactory,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    ): FlightExplorer {
        val store = FlightLocalStore(driverFactory.createDriver())
        val api = FlightsApi(baseUrl)
        val repository = FlightRepository(api, store)
        return FlightExplorer(repository = repository, scope = scope)
    }
}

object ApiBaseUrls {
    const val ANDROID_EMULATOR = "http://10.0.2.2:8080"
    const val IOS_SIMULATOR = "http://127.0.0.1:8080"
}
