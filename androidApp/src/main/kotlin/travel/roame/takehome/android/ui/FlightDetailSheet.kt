package travel.roame.takehome.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import travel.roame.takehome.model.Flight

/**
 * Bottom sheet for a selected flight: details + embedded route map + Save/Remove.
 * Opens near-full height with Save pinned so it stays on screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailSheet(
    flight: Flight,
    saved: Boolean,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RoameColors.Surface,
        shape = RoameShapes.Panel,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .semantics { contentDescription = "Selected flight details" },
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "${flight.origin} → ${flight.destination}",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp,
                    color = RoameColors.Ink,
                )
                Text(
                    "${flight.originCity} → ${flight.destinationCity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoameColors.Muted,
                )

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoameShapes.Card),
                ) {
                    FlightMap(
                        clusters = emptyList(),
                        selectedFlight = flight,
                        onClusterTap = {},
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                    )
                }

                DetailRow(label = "Departure", value = flight.departureDate)
                DetailRow(label = "Cabin", value = flight.cabin)
                DetailRow(label = "Program", value = flight.program)
                Text(
                    "%,d points".format(flight.points),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = RoameColors.Ink,
                )
                Spacer(Modifier.height(8.dp))
            }

            // Pinned action — always visible at the bottom of the sheet.
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 20.dp),
            ) {
                if (saved) {
                    OutlinedButton(
                        onClick = onRemove,
                        shape = RoameShapes.Pill,
                        border = BorderStroke(1.dp, RoameColors.Pink),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoameColors.Pink),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Remove saved flight" },
                    ) {
                        Text("Remove", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = onSave,
                        shape = RoameShapes.Pill,
                        colors = ButtonDefaults.buttonColors(containerColor = RoameColors.Pink),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Save flight" },
                    ) {
                        Text("Save", fontWeight = FontWeight.SemiBold, color = RoameColors.Ink)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = RoameColors.Muted)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
