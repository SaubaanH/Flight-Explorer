import SwiftUI
import WebKit
import Shared

/// Detail-sheet route map — same MapLibre `map.html` as Android.
/// MapKit could not keep both airports in frame across the dateline (camera and
/// pins landed on different world copies). Sketch covers the brief tile load.
struct RoutePreviewMapView: View {
    let flight: Flight
    @State private var mapReady = false

    var body: some View {
        ZStack {
            RouteSketchView(flight: flight)
                .opacity(mapReady ? 0 : 1)

            MapLibreRouteWebView(flight: flight) {
                withAnimation(.easeOut(duration: 0.2)) { mapReady = true }
            }
            .opacity(mapReady ? 1 : 0)
        }
        .onChange(of: flight.id) { _, _ in
            mapReady = false
        }
        .accessibilityLabel("Route map from \(flight.origin) to \(flight.destination)")
    }
}

struct RouteSketchView: View {
    let flight: Flight

    var body: some View {
        ZStack {
            Color(red: 0.94, green: 0.93, blue: 0.92)

            Canvas { context, size in
                let layout = ShortPathLayout(flight: flight, size: size, inset: 28)
                var glow = Path()
                glow.move(to: layout.origin)
                glow.addLine(to: layout.destination)
                context.stroke(
                    glow,
                    with: .color(RoameTheme.pink.opacity(0.3)),
                    style: StrokeStyle(lineWidth: 6, lineCap: .round, dash: [10, 8])
                )
                var line = Path()
                line.move(to: layout.origin)
                line.addLine(to: layout.destination)
                context.stroke(
                    line,
                    with: .color(RoameTheme.pink),
                    style: StrokeStyle(lineWidth: 3, lineCap: .round)
                )
                drawDot(context, at: layout.origin)
                drawDot(context, at: layout.destination)
            }

            VStack {
                HStack {
                    Spacer()
                    Text(flight.destination)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(RoameTheme.ink)
                }
                Spacer()
                HStack {
                    Text(flight.origin)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(RoameTheme.ink)
                    Spacer()
                }
            }
            .padding(12)
        }
    }

    private func drawDot(_ context: GraphicsContext, at point: CGPoint) {
        let outer = CGRect(x: point.x - 6, y: point.y - 6, width: 12, height: 12)
        context.fill(Path(ellipseIn: outer), with: .color(RoameTheme.pink))
        context.fill(Path(ellipseIn: outer.insetBy(dx: 3.5, dy: 3.5)), with: .color(.white))
    }
}

private struct ShortPathLayout {
    let origin: CGPoint
    let destination: CGPoint

    init(flight: Flight, size: CGSize, inset: CGFloat) {
        var delta = flight.destinationLongitude - flight.originLongitude
        while delta > 180 { delta -= 360 }
        while delta < -180 { delta += 360 }
        let destLng = flight.originLongitude + delta

        let minLat = min(flight.originLatitude, flight.destinationLatitude)
        let maxLat = max(flight.originLatitude, flight.destinationLatitude)
        let minLng = min(flight.originLongitude, destLng)
        let maxLng = max(flight.originLongitude, destLng)
        let latPad = max((maxLat - minLat) * 0.28, 6)
        let lngPad = max((maxLng - minLng) * 0.28, 8)
        let left = minLng - lngPad
        let right = maxLng + lngPad
        let top = maxLat + latPad
        let bottom = minLat - latPad
        let spanLng = max(right - left, 1e-6)
        let spanLat = max(top - bottom, 1e-6)
        let draw = CGRect(origin: .zero, size: size).insetBy(dx: inset, dy: inset)

        func project(lat: Double, lng: Double) -> CGPoint {
            CGPoint(
                x: draw.minX + CGFloat((lng - left) / spanLng) * draw.width,
                y: draw.minY + CGFloat((top - lat) / spanLat) * draw.height
            )
        }

        origin = project(lat: flight.originLatitude, lng: flight.originLongitude)
        destination = project(lat: flight.destinationLatitude, lng: destLng)
    }
}

// MARK: - MapLibre (shared with Android)

private struct MapLibreRouteWebView: UIViewRepresentable {
    let flight: Flight
    var onReady: () -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onReady: onReady)
    }

    func makeUIView(context: Context) -> WKWebView {
        let content = WKUserContentController()
        content.add(context.coordinator, name: "mapReady")

        let config = WKWebViewConfiguration()
        config.userContentController = content
        let page = WKWebpagePreferences()
        page.allowsContentJavaScript = true
        config.defaultWebpagePreferences = page

        let webView = WKWebView(frame: .zero, configuration: config)
        webView.isOpaque = false
        webView.backgroundColor = UIColor(red: 0.94, green: 0.93, blue: 0.92, alpha: 1)
        webView.scrollView.isScrollEnabled = false
        webView.scrollView.bounces = false
        webView.navigationDelegate = context.coordinator

        if let url = Bundle.main.url(forResource: "map", withExtension: "html", subdirectory: "MapAssets") {
            webView.loadFileURL(url, allowingReadAccessTo: url.deletingLastPathComponent())
        }
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        context.coordinator.onReady = onReady
        context.coordinator.flight = flight
        context.coordinator.pushRoute(to: webView)
    }

    static func dismantleUIView(_ uiView: WKWebView, coordinator: Coordinator) {
        uiView.configuration.userContentController.removeScriptMessageHandler(forName: "mapReady")
    }

    final class Coordinator: NSObject, WKNavigationDelegate, WKScriptMessageHandler {
        var flight: Flight?
        var onReady: () -> Void
        private var pageReady = false
        private var didSignal = false

        init(onReady: @escaping () -> Void) {
            self.onReady = onReady
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            pageReady = true
            pushRoute(to: webView)
            // Fallback if idle never fires (cached tiles / offline style).
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) { [weak self] in
                self?.signalReady()
            }
        }

        func userContentController(
            _ userContentController: WKUserContentController,
            didReceive message: WKScriptMessage
        ) {
            if message.name == "mapReady" {
                signalReady()
            }
        }

        func pushRoute(to webView: WKWebView) {
            guard pageReady, let flight else { return }
            didSignal = false
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

        private func signalReady() {
            guard !didSignal else { return }
            didSignal = true
            onReady()
        }
    }
}
