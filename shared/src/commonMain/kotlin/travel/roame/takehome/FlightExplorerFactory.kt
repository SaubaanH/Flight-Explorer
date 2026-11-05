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
    ): FlightExplorer = create(
        baseUrl = baseUrl,
        driverFactory = driverFactory,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )

    fun create(
        baseUrl: String,
        driverFactory: DatabaseDriverFactory,
        scope: CoroutineScope,
    ): FlightExplorer {
        val store = FlightLocalStore(driverFactory.createDriver())
        val api = FlightsApi(baseUrl)
        val repository = FlightRepository(api, store)
        return FlightExplorer(repository = repository, scope = scope)
    }
}

object ApiBaseUrls {
    /**
     * Emulator loopback + `adb reverse tcp:8080 tcp:8080`.
     * Prefer this over `10.0.2.2` — on newer Android Emulator networking (`netsimd`)
     * OkHttp often connect-timeouts to `10.0.2.2` even while host/shell can reach the API.
     */
    const val ANDROID_EMULATOR = "http://127.0.0.1:8080"
    /** Legacy alias (host loopback via emulator gateway). Prefer [ANDROID_EMULATOR]. */
    const val ANDROID_EMULATOR_GATEWAY = "http://10.0.2.2:8080"
    const val IOS_SIMULATOR = "http://127.0.0.1:8080"
}
