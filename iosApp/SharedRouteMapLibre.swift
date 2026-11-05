import WebKit
import Shared

/// One prewarmed MapLibre WebView shared by detail sheets.
/// Cold-start (JS + style) happens at launch so opening a flight only runs fitBounds.
@MainActor
final class SharedRouteMapLibre: NSObject, WKNavigationDelegate, WKScriptMessageHandler {
    static let shared = SharedRouteMapLibre()

    private(set) var webView: WKWebView!
    private(set) var isEngineReady = false
    private var pendingFlight: Flight?
    private var onRouteReady: (() -> Void)?

    private override init() {
        super.init()
        let content = WKUserContentController()
        content.add(self, name: "mapReady")

        let config = WKWebViewConfiguration()
        config.userContentController = content
        config.processPool = Self.processPool
        let page = WKWebpagePreferences()
        page.allowsContentJavaScript = true
        config.defaultWebpagePreferences = page

        let view = WKWebView(frame: CGRect(x: 0, y: 0, width: 360, height: 180), configuration: config)
        view.isOpaque = false
        view.backgroundColor = UIColor(red: 0.94, green: 0.93, blue: 0.92, alpha: 1)
        view.scrollView.isScrollEnabled = false
        view.scrollView.bounces = false
        view.navigationDelegate = self
        webView = view

        if let url = Bundle.main.url(forResource: "map", withExtension: "html", subdirectory: "MapAssets") {
            view.loadFileURL(url, allowingReadAccessTo: url.deletingLastPathComponent())
        }
    }

    /// Kick engine load early (call from app / Discover appear).
    func prewarm() {
        _ = webView
    }

    func show(flight: Flight, onReady: @escaping () -> Void) {
        pendingFlight = flight
        onRouteReady = onReady
        guard isEngineReady else { return }
        pushRoute(flight)
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        isEngineReady = true
        if let flight = pendingFlight {
            pushRoute(flight)
        }
    }

    func userContentController(
        _ userContentController: WKUserContentController,
        didReceive message: WKScriptMessage
    ) {
        if message.name == "mapReady" {
            onRouteReady?()
            onRouteReady = nil
        }
    }

    private func pushRoute(_ flight: Flight) {
        let route: [String: Any] = [
            "origin": flight.origin,
            "destination": flight.destination,
            "oLat": flight.originLatitude,
            "oLng": flight.originLongitude,
            "dLat": flight.destinationLatitude,
            "dLng": flight.destinationLongitude,
        ]
        guard let data = try? JSONSerialization.data(withJSONObject: route),
              let json = String(data: data, encoding: .utf8) else { return }
        webView.evaluateJavaScript(
            "window.updateFlightMap && window.updateFlightMap([], \(json));",
            completionHandler: nil
        )
    }

    private static let processPool = WKProcessPool()
}
