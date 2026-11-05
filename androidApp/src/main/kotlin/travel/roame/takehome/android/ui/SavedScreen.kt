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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Saved flights", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Independent of discovery filters. Available offline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (savedFlights.isEmpty()) {
                Text(
                    "No saved flights yet. Save one from Discover.",
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 140.dp),
                ) {
                    items(savedFlights, key = { it.id }) { flight ->
                        FlightRow(
                            flight = flight,
                            selected = flight.id == selectedFlight?.id,
                            saved = true,
                            onClick = { explorer.selectFlight(flight.id) },
                        )
                    }
                }
            }
            val selectedSaved = selectedFlight?.takeIf { sel ->
                savedFlights.any { it.id == sel.id }
            }
            selectedSaved?.let { flight ->
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .fillMaxWidth(),
                ) {
                    SelectionCard(
                        flight = flight,
                        saved = true,
                        onSave = { explorer.saveFlight(flight) },
                        onRemove = { explorer.removeSaved(flight.id) },
                        onDismiss = { explorer.selectFlight(null) },
                    )
                }
            }
        }
    }
}
