package travel.roame.takehome.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import travel.roame.takehome.ApiBaseUrls
import travel.roame.takehome.FlightExplorerFactory
import travel.roame.takehome.android.ui.FlightExplorerApp
import travel.roame.takehome.android.ui.RoameTheme
import travel.roame.takehome.data.DatabaseDriverFactory

class MainActivity : ComponentActivity() {
    private var explorer: travel.roame.takehome.FlightExplorer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appExplorer = remember {
                FlightExplorerFactory.create(
                    baseUrl = ApiBaseUrls.ANDROID_EMULATOR,
                    driverFactory = DatabaseDriverFactory(applicationContext),
                ).also { explorer = it }
            }
            RoameTheme {
                FlightExplorerApp(explorer = appExplorer)
            }
        }
    }

    override fun onDestroy() {
        explorer?.close()
        super.onDestroy()
    }
}
