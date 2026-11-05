package travel.roame.takehome.android.ui

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject
import travel.roame.takehome.model.DestinationCluster
import travel.roame.takehome.model.Flight

/**
 * MapLibre GL JS inside a WebView — no Google API key, pan/zoom, destination markers
 * (count + lowest points), and selected-flight route connector.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FlightMap(
    clusters: List<DestinationCluster>,
    selectedFlight: Flight?,
    onClusterTap: (DestinationCluster) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clustersState = rememberUpdatedState(clusters)
    val onTapState = rememberUpdatedState(onClusterTap)
    val bridge = remember {
        object {
            @JavascriptInterface
            fun onMarkerClick(code: String) {
                clustersState.value.firstOrNull { it.destination == code }?.let { onTapState.value(it) }
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                addJavascriptInterface(bridge, "AndroidBridge")
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript(buildUpdateJs(clustersState.value, selectedFlight), null)
                    }
                }
                loadDataWithBaseURL(
                    "https://localhost/",
                    MAP_HTML,
                    "text/html",
                    "UTF-8",
                    null,
                )
            }
        },
        update = { webView ->
            webView.evaluateJavascript(buildUpdateJs(clusters, selectedFlight), null)
        },
    )
}

private fun buildUpdateJs(clusters: List<DestinationCluster>, selected: Flight?): String {
    val markers = JSONArray()
    clusters.forEach { cluster ->
        markers.put(
            JSONObject()
                .put("code", cluster.destination)
                .put("city", cluster.destinationCity)
                .put("lat", cluster.latitude)
                .put("lng", cluster.longitude)
                .put("count", cluster.flightCount)
                .put("minPoints", cluster.lowestPoints),
        )
    }
    val route = if (selected == null) {
        "null"
    } else {
        JSONObject()
            .put("origin", selected.origin)
            .put("destination", selected.destination)
            .put("oLat", selected.originLatitude)
            .put("oLng", selected.originLongitude)
            .put("dLat", selected.destinationLatitude)
            .put("dLng", selected.destinationLongitude)
            .toString()
    }
    return "window.updateFlightMap && window.updateFlightMap($markers, $route);"
}

private const val MAP_HTML = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1"/>
<link href="https://unpkg.com/maplibre-gl@4.7.1/dist/maplibre-gl.css" rel="stylesheet"/>
<script src="https://unpkg.com/maplibre-gl@4.7.1/dist/maplibre-gl.js"></script>
<style>
  html, body, #map { margin:0; padding:0; height:100%; width:100%; }
  .marker {
    background:#0B6E4F; color:white; border-radius:12px; padding:4px 8px;
    font: 600 11px/1.2 -apple-system, BlinkMacSystemFont, sans-serif;
    box-shadow:0 1px 4px rgba(0,0,0,.35); white-space:nowrap;
  }
</style>
</head>
<body>
<div id="map"></div>
<script>
  const map = new maplibregl.Map({
    container: 'map',
    style: 'https://demotiles.maplibre.org/style.json',
    center: [0, 20],
    zoom: 1.2
  });
  map.addControl(new maplibregl.NavigationControl(), 'top-right');
  let markerObjs = [];
  let routeSourceReady = false;
  let pending = null;

  function ensureRouteLayer() {
    if (routeSourceReady) return;
    map.addSource('route', { type: 'geojson', data: { type: 'FeatureCollection', features: [] } });
    map.addLayer({
      id: 'route-line',
      type: 'line',
      source: 'route',
      paint: { 'line-color': '#0B6E4F', 'line-width': 3 }
    });
    routeSourceReady = true;
  }

  function applyUpdate(markers, route) {
    markerObjs.forEach(m => m.remove());
    markerObjs = [];
    const bounds = new maplibregl.LngLatBounds();
    markers.forEach(m => {
      const el = document.createElement('div');
      el.className = 'marker';
      el.textContent = m.code + ' · ' + m.count + ' · ' + Number(m.minPoints).toLocaleString() + 'pts';
      el.setAttribute('aria-label', m.city + ' ' + m.count + ' flights from ' + m.minPoints + ' points');
      el.onclick = () => AndroidBridge.onMarkerClick(m.code);
      const marker = new maplibregl.Marker({ element: el }).setLngLat([m.lng, m.lat]).addTo(map);
      markerObjs.push(marker);
      bounds.extend([m.lng, m.lat]);
    });
    if (map.isStyleLoaded()) {
      ensureRouteLayer();
      if (route) {
        map.getSource('route').setData({
          type: 'Feature',
          geometry: {
            type: 'LineString',
            coordinates: [[route.oLng, route.oLat], [route.dLng, route.dLat]]
          }
        });
        bounds.extend([route.oLng, route.oLat]);
        bounds.extend([route.dLng, route.dLat]);
      } else {
        map.getSource('route').setData({ type: 'FeatureCollection', features: [] });
      }
    }
    if (!bounds.isEmpty()) {
      map.fitBounds(bounds, { padding: 48, maxZoom: 5, duration: 500 });
    }
  }

  window.updateFlightMap = function(markers, route) {
    if (!map.isStyleLoaded()) { pending = { markers, route }; return; }
    applyUpdate(markers, route);
  };
  map.on('load', () => {
    if (pending) { applyUpdate(pending.markers, pending.route); pending = null; }
  });
</script>
</body>
</html>
"""
