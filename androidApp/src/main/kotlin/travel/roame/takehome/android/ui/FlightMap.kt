package travel.roame.takehome.android.ui

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log
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
 * MapLibre GL JS inside a WebView — JS/CSS bundled in assets (no CDN),
 * destination markers (count + lowest points), selected / focus routes.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FlightMap(
    clusters: List<DestinationCluster>,
    routes: List<Flight>,
    onClusterTap: (DestinationCluster) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clustersState = rememberUpdatedState(clusters)
    val routesState = rememberUpdatedState(routes)
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
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(AndroidColor.parseColor("#F0EEEA"))
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                addJavascriptInterface(bridge, "AndroidBridge")
                tag = ""
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        Log.d(
                            "FlightMap",
                            "${consoleMessage?.message()} @${consoleMessage?.lineNumber()}",
                        )
                        return true
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val js = buildUpdateJs(clustersState.value, routesState.value)
                        view?.tag = js
                        view?.evaluateJavascript(js, null)
                    }
                }
                loadUrl("file:///android_asset/map.html")
            }
        },
        update = { webView ->
            val js = buildUpdateJs(clusters, routes)
            if (webView.tag != js) {
                webView.tag = js
                webView.evaluateJavascript(js, null)
            }
        },
    )
}

private fun buildUpdateJs(clusters: List<DestinationCluster>, routes: List<Flight>): String {
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
    val routesJson = JSONArray()
    routes.forEach { flight ->
        routesJson.put(
            JSONObject()
                .put("origin", flight.origin)
                .put("destination", flight.destination)
                .put("oLat", flight.originLatitude)
                .put("oLng", flight.originLongitude)
                .put("dLat", flight.destinationLatitude)
                .put("dLng", flight.destinationLongitude),
        )
    }
    val routesArg = if (routesJson.length() == 0) "null" else routesJson.toString()
    return "window.updateFlightMap && window.updateFlightMap($markers, $routesArg);"
}
