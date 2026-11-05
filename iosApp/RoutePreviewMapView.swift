import SwiftUI
import WebKit
import Shared

/// Detail-sheet route map using the prewarmed shared MapLibre engine.
/// No sketch crossfade — beige placeholder matches the map chrome until the
/// fitted camera is ready, then the real map appears in place.
struct RoutePreviewMapView: View {
    let flight: Flight

    var body: some View {
        ZStack {
            Color(red: 0.94, green: 0.93, blue: 0.92)
            SharedMapLibreAttach(flight: flight)
        }
        .accessibilityLabel("Route map from \(flight.origin) to \(flight.destination)")
    }
}

/// Re-parents the shared WKWebView into the sheet slot.
private struct SharedMapLibreAttach: UIViewRepresentable {
    let flight: Flight

    func makeUIView(context: Context) -> UIView {
        let container = UIView()
        container.backgroundColor = UIColor(red: 0.94, green: 0.93, blue: 0.92, alpha: 1)
        attach(to: container)
        return container
    }

    func updateUIView(_ container: UIView, context: Context) {
        attach(to: container)
        SharedRouteMapLibre.shared.show(flight: flight) {}
        // MapLibre needs an explicit resize after re-parenting into the sheet.
        SharedRouteMapLibre.shared.webView.evaluateJavaScript(
            "window.map && window.map.resize && window.map.resize();",
            completionHandler: nil
        )
    }

    private func attach(to container: UIView) {
        let webView = SharedRouteMapLibre.shared.webView!
        if webView.superview !== container {
            webView.removeFromSuperview()
            webView.translatesAutoresizingMaskIntoConstraints = false
            container.addSubview(webView)
            NSLayoutConstraint.activate([
                webView.topAnchor.constraint(equalTo: container.topAnchor),
                webView.bottomAnchor.constraint(equalTo: container.bottomAnchor),
                webView.leadingAnchor.constraint(equalTo: container.leadingAnchor),
                webView.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            ])
        }
    }
}
