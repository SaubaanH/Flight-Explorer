import SwiftUI
import WebKit
import Shared

/// Detail-sheet route preview — same MapLibre `map.html` as Android.
/// Avoids MapKit geodesic framing (polar arcs like JFK → HKG).
struct RoutePreviewMapView: UIViewRepresentable {
    let flight: Flight

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        let page = WKWebpagePreferences()
        page.allowsContentJavaScript = true
        config.defaultWebpagePreferences = page
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.isOpaque = false
        webView.backgroundColor = UIColor(red: 0.94, green: 0.93, blue: 0.92, alpha: 1)
        webView.scrollView.isScrollEnabled = false
        webView.scrollView.bounces = false
        webView.navigationDelegate = context.coordinator

        if let url = Bundle.main.url(forResource: "map", withExtension: "html", subdirectory: "MapAssets")
            ?? Bundle.main.url(forResource: "map", withExtension: "html") {
            let access = url.deletingLastPathComponent()
            webView.loadFileURL(url, allowingReadAccessTo: access)
        }
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        context.coordinator.flight = flight
        context.coordinator.pushRoute(to: webView)
    }

    final class Coordinator: NSObject, WKNavigationDelegate {
        var flight: Flight?
        private var ready = false

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            ready = true
            pushRoute(to: webView)
        }

        func pushRoute(to webView: WKWebView) {
            guard ready, let flight else { return }
            let route: [String: Any] = [
                "origin": flight.origin,
                "destination": flight.destination,
                "oLat": flight.originLatitude,
                "oLng": flight.originLongitude,
                "dLat": flight.destinationLatitude,
                "dLng": flight.destinationLongitude,
            ]
            guard let data = try? JSONSerialization.data(withJSONObject: route),
                  let routeJson = String(data: data, encoding: .utf8) else { return }
            let js = "window.updateFlightMap && window.updateFlightMap([], \(routeJson));"
            webView.evaluateJavaScript(js, completionHandler: nil)
        }
    }
}
