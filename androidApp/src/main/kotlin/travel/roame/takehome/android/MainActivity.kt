package travel.roame.takehome.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import travel.roame.takehome.ApiBaseUrls
import travel.roame.takehome.FlightExplorerFactory
import travel.roame.takehome.android.ui.FlightExplorerApp
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
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF0B6E4F),
                    secondary = Color(0xFF1B9AAA),
                    tertiary = Color(0xFFC45D00),
                    background = Color(0xFFF6F4EF),
                    surface = Color(0xFFFFFCF7),
                ),
            ) {
                FlightExplorerApp(explorer = appExplorer)
            }
        }
    }

    override fun onDestroy() {
        explorer?.close()
        super.onDestroy()
    }
}
