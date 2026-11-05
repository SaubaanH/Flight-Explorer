package travel.roame.takehome.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import travel.roame.takehome.FlightExplorer
import travel.roame.takehome.model.Flight

@Composable
fun SavedScreen(
    savedFlights: List<Flight>,
    selectedFlight: Flight?,
    explorer: FlightExplorer,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var detailFlightId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedFlight?.id) {
        if (selectedFlight == null) detailFlightId = null
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RoameBrandHeader(
            title = "Saved flights",
            subtitle = "Independent of discovery filters. Available offline.",
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (savedFlights.isEmpty()) {
                Text(
                    "No saved flights yet. Save one from Discover.",
                    modifier = Modifier.align(Alignment.Center),
                    color = RoameColors.Muted,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 160.dp),
                ) {
                    items(savedFlights, key = { it.id }) { flight ->
                        FlightRow(
                            flight = flight,
                            selected = flight.id == selectedFlight?.id,
                            saved = true,
                            onClick = {
                                SharedDetailRouteMap.prepare(context, flight)
                                explorer.selectFlight(flight.id)
                                detailFlightId = flight.id
                            },
                        )
                    }
                }
            }
        }
    }

    val detailFlight = detailFlightId?.let { id ->
        savedFlights.firstOrNull { it.id == id }
            ?: selectedFlight?.takeIf { it.id == id }
    }
    detailFlight?.let { flight ->
        FlightDetailSheet(
            flight = flight,
            saved = true,
            onSave = { explorer.saveFlight(flight) },
            onRemove = {
                explorer.removeSaved(flight.id)
                detailFlightId = null
                explorer.selectFlight(null)
            },
            onDismiss = {
                detailFlightId = null
                explorer.selectFlight(null)
            },
        )
    }
}
