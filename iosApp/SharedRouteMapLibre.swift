import UIKit
import WebKit
import Shared

/// One prewarmed MapLibre WebView for detail sheets.
/// Call `prepare(flight)` on tap *before* the sheet animates so fitBounds finishes
/// during the slower open.
@MainActor
final class SharedRouteMapLibre: NSObject, WKNavigationDelegate, WKScriptMessageHandler {
    static let shared = SharedRouteMapLibre()

    private(set) var webView: WKWebView!
    /// Hidden host that keeps the WebView alive / layoutable while the sheet is closed.
    private let parkingLot = UIView(frame: CGRect(x: 0, y: 0, width: 360, height: 180))

    private(set) var isEngineReady = false
    private(set) var preparedFlightId: String?
    private(set) var isPreparedRouteReady = false

    private var pendingFlight: Flight?
    private var readyHandlers: [() -> Void] = []

    private override init() {
        super.init()
        parkingLot.isHidden = true
        parkingLot.alpha = 0

        let content = WKUserContentController()
        content.add(self, name: "mapReady")

        let config = WKWebViewConfiguration()
        config.userContentController = content
        config.processPool = Self.processPool
        let page = WKWebpagePreferences()
        page.allowsContentJavaScript = true
        config.defaultWebpagePreferences = page

        let view = WKWebView(frame: parkingLot.bounds, configuration: config)
        view.isOpaque = false
        view.backgroundColor = UIColor(red: 0.94, green: 0.93, blue: 0.92, alpha: 1)
        view.scrollView.isScrollEnabled = false
        view.scrollView.bounces = false
        view.navigationDelegate = self
        view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        webView = view
        park()

        if let url = Bundle.main.url(forResource: "map", withExtension: "html", subdirectory: "MapAssets") {
            view.loadFileURL(url, allowingReadAccessTo: url.deletingLastPathComponent())
        }
    }

    func prewarm() {
        ensureParkedInWindow()
        _ = webView
    }

    /// Start fitting the route immediately (call on row tap, before sheet opens).
    func prepare(_ flight: Flight) {
        ensureParkedInWindow()
        if preparedFlightId != flight.id {
            preparedFlightId = flight.id
            isPreparedRouteReady = false
        }
        pendingFlight = flight
        guard isEngineReady else { return }
        pushRoute(flight)
    }

    func onReady(_ handler: @escaping () -> Void) {
        if isPreparedRouteReady, pendingFlight != nil {
            handler()
        } else {
            readyHandlers.append(handler)
        }
    }

    /// Move the shared WebView into the sheet's map slot.
    func attach(to container: UIView) {
        let view = webView!
        if view.superview !== container {
            view.removeFromSuperview()
            view.translatesAutoresizingMaskIntoConstraints = false
            container.addSubview(view)
            NSLayoutConstraint.activate([
                view.topAnchor.constraint(equalTo: container.topAnchor),
                view.bottomAnchor.constraint(equalTo: container.bottomAnchor),
                view.leadingAnchor.constraint(equalTo: container.leadingAnchor),
                view.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            ])
        }
        container.layoutIfNeeded()
        view.evaluateJavaScript(
            """
            if (window.map && window.map.resize) {
              window.map.resize();
              var el = document.getElementById('map');
              if (el) el.classList.add('is-ready');
            }
            """,
            completionHandler: nil
        )
        // Avoid re-fitting (and re-hiding the canvas) if this route is already ready.
        if let flight = pendingFlight,
           preparedFlightId != flight.id || !isPreparedRouteReady {
            pushRoute(flight)
        }
    }

    /// Return the WebView to the offscreen parking lot when the sheet closes.
    func park() {
        let view = webView!
        if view.superview !== parkingLot {
            view.removeFromSuperview()
            view.frame = parkingLot.bounds
            view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            parkingLot.addSubview(view)
        }
        ensureParkedInWindow()
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
        guard message.name == "mapReady" else { return }
        isPreparedRouteReady = true
        let handlers = readyHandlers
        readyHandlers.removeAll()
        handlers.forEach { $0() }
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

    private func ensureParkedInWindow() {
        guard parkingLot.superview == nil else { return }
        guard let scene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first(where: { $0.activationState == .foregroundActive }),
            let window = scene.windows.first(where: \.isKeyWindow) ?? scene.windows.first
        else { return }
        parkingLot.frame = CGRect(x: -1000, y: -1000, width: 360, height: 180)
        window.addSubview(parkingLot)
        window.sendSubviewToBack(parkingLot)
    }

    private static let processPool = WKProcessPool()
}
