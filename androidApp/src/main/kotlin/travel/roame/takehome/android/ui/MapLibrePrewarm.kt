package travel.roame.takehome.android.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Off-screen MapLibre bootstrap so the first detail-sheet open doesn't pay the
 * full JS/style cold start (tiles still fetch, but the engine is warm).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapLibrePrewarm() {
    AndroidView(
        modifier = Modifier.size(1.dp),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(1, 1)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                loadUrl("file:///android_asset/map.html")
            }
        },
    )
}
