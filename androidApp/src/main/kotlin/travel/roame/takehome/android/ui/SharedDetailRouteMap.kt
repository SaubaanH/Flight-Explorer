package travel.roame.takehome.android.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject
import travel.roame.takehome.model.Flight

/**
 * Single MapLibre WebView for detail sheets. Call [prepare] on row tap so
 * fitBounds runs while the sheet animates open.
 */
@SuppressLint("SetJavaScriptEnabled")
object SharedDetailRouteMap {
    @Volatile
    private var webView: WebView? = null

    @Volatile
    private var engineReady = false

    @Volatile
    private var pendingFlight: Flight? = null

    @Volatile
    private var preparedFlightId: String? = null

    @Volatile
    private var routeReady = false

    fun ensure(context: Context): WebView {
        webView?.let { return it }
        val app = context.applicationContext
        return WebView(app).apply {
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
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    engineReady = true
                    pendingFlight?.let { pushRoute(it) }
                }
            }
            // Give the offscreen engine a real layout size so MapLibre can fitBounds.
            layout(0, 0, 1080, 540)
            loadUrl("file:///android_asset/map.html")
            webView = this
        }
    }

    fun prewarm(context: Context) {
        ensure(context)
    }

    fun prepare(context: Context, flight: Flight) {
        if (preparedFlightId != flight.id) {
            preparedFlightId = flight.id
            routeReady = false
        }
        pendingFlight = flight
        ensure(context)
        if (engineReady) pushRoute(flight)
    }

    fun attach(container: ViewGroup, context: Context): WebView {
        val map = ensure(context)
        (map.parent as? ViewGroup)?.removeView(map)
        container.addView(
            map,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        map.post {
            map.evaluateJavascript(
                """
                if (window.map && window.map.resize) {
                  window.map.resize();
                  var el = document.getElementById('map');
                  if (el) el.classList.add('is-ready');
                }
                """.trimIndent(),
                null,
            )
            val flight = pendingFlight
            if (flight != null && (!routeReady || preparedFlightId != flight.id)) {
                pushRoute(flight)
            }
        }
        return map
    }

    private fun pushRoute(flight: Flight) {
        val map = webView ?: return
        preparedFlightId = flight.id
        routeReady = false
        val route = JSONObject()
            .put("origin", flight.origin)
            .put("destination", flight.destination)
            .put("oLat", flight.originLatitude)
            .put("oLng", flight.originLongitude)
            .put("dLat", flight.destinationLatitude)
            .put("dLng", flight.destinationLongitude)
            .toString()
        // Mark ready on next idle via JS; for re-attach we also force is-ready after resize.
        map.evaluateJavascript(
            """
            window.updateFlightMap && window.updateFlightMap([], $route);
            """.trimIndent(),
            null,
        )
        // Assume fitted shortly — sheet animation covers residual tile time.
        map.postDelayed({ routeReady = true }, 450)
    }
}
