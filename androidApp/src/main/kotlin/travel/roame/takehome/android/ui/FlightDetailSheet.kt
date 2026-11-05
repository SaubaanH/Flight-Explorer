package travel.roame.takehome.android.ui

import android.graphics.drawable.ColorDrawable
import android.graphics.Color as AndroidColor
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import travel.roame.takehome.model.Flight

private val SheetOpenMillis = 420
private val SheetCloseMillis = 300

/**
 * Bottom sheet for a selected flight: details + route map + Save/Remove.
 * Opens with a slower slide so [SharedDetailRouteMap.prepare] can finish fitting.
 */
@Composable
fun FlightDetailSheet(
    flight: Flight,
    saved: Boolean,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(flight.id) {
        visible = true
    }

    Dialog(
        onDismissRequest = {
            visible = false
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.setBackgroundDrawable(
                ColorDrawable(AndroidColor.TRANSPARENT),
            )
        }

        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(SheetOpenMillis / 2)),
                exit = fadeOut(tween(SheetCloseMillis / 2)),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                visible = false
                                onDismiss()
                            },
                        ),
                )
            }

            AnimatedVisibility(
                visible = visible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(
                    animationSpec = tween(SheetOpenMillis),
                    initialOffsetY = { it },
                ) + fadeIn(tween(SheetOpenMillis / 2)),
                exit = slideOutVertically(
                    animationSpec = tween(SheetCloseMillis),
                    targetOffsetY = { it },
                ) + fadeOut(tween(SheetCloseMillis / 2)),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Selected flight details" },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = RoameColors.Surface,
                    shadowElevation = 6.dp,
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier
                                .padding(top = 10.dp, bottom = 2.dp)
                                .align(Alignment.CenterHorizontally)
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(RoameColors.Muted.copy(alpha = 0.35f)),
                        )

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

                        DetailRouteMap(flight = flight)

                        DetailRow(label = "Departure", value = flight.departureDate)
                        DetailRow(label = "Cabin", value = flight.cabin)
                        DetailRow(label = "Program", value = flight.program)
                        Text(
                            "%,d points".format(flight.points),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = RoameColors.Ink,
                        )

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
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRouteMap(flight: Flight) {
    val context = LocalContext.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoameShapes.Card)
            .background(Color(0xFFF0EEEA)),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                FrameLayout(ctx).also { host ->
                    SharedDetailRouteMap.attach(host, ctx)
                }
            },
            update = { host ->
                SharedDetailRouteMap.prepare(context, flight)
                SharedDetailRouteMap.attach(host, context)
            },
        )
        DisposableEffect(flight.id) {
            onDispose { /* keep shared WebView warm */ }
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
