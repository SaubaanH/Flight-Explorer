package travel.roame.takehome.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import travel.roame.takehome.model.Flight
import kotlin.math.max

/**
 * Lightweight route sketch for the detail sheet — avoids a second MapLibre WebView.
 * Uses the short longitude path so Pacific routes aren't sketched the long way.
 */
@Composable
fun RoutePreview(
    flight: Flight,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .background(Color(0xFFF0EEEA))
            .semantics {
                contentDescription = "Route from ${flight.origin} to ${flight.destination}"
            },
    ) {
        Canvas(Modifier.fillMaxSize().padding(24.dp)) {
            var delta = flight.destinationLongitude - flight.originLongitude
            while (delta > 180) delta -= 360
            while (delta < -180) delta += 360
            val destLng = flight.originLongitude + delta

            val minLat = minOf(flight.originLatitude, flight.destinationLatitude)
            val maxLat = maxOf(flight.originLatitude, flight.destinationLatitude)
            val minLng = minOf(flight.originLongitude, destLng)
            val maxLng = maxOf(flight.originLongitude, destLng)
            val latPad = max((maxLat - minLat) * 0.28, 6.0)
            val lngPad = max((maxLng - minLng) * 0.28, 8.0)
            val left = minLng - lngPad
            val right = maxLng + lngPad
            val top = maxLat + latPad
            val bottom = minLat - latPad
            val spanLng = (right - left).coerceAtLeast(1e-6)
            val spanLat = (top - bottom).coerceAtLeast(1e-6)

            fun project(lat: Double, lng: Double): Offset {
                val x = ((lng - left) / spanLng).toFloat() * size.width
                val y = ((top - lat) / spanLat).toFloat() * size.height
                return Offset(x, y)
            }

            val origin = project(flight.originLatitude, flight.originLongitude)
            val dest = project(flight.destinationLatitude, destLng)
            val pink = Color(0xFFE83A6F)

            drawLine(
                color = pink.copy(alpha = 0.35f),
                start = origin,
                end = dest,
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f)),
            )
            drawLine(
                color = pink,
                start = origin,
                end = dest,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(color = pink, radius = 7.dp.toPx(), center = origin)
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = origin)
            drawCircle(color = pink, radius = 7.dp.toPx(), center = dest)
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = dest)
        }

        Text(
            flight.origin,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            color = RoameColors.Ink,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
        Text(
            flight.destination,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            color = RoameColors.Ink,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
    }
}
