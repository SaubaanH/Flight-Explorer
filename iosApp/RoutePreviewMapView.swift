import SwiftUI
import Shared

/// Detail-sheet route map — shared prewarmed MapLibre engine (prepared on tap).
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

private struct SharedMapLibreAttach: UIViewRepresentable {
    let flight: Flight

    func makeUIView(context: Context) -> UIView {
        let container = UIView()
        container.backgroundColor = UIColor(red: 0.94, green: 0.93, blue: 0.92, alpha: 1)
        SharedRouteMapLibre.shared.attach(to: container)
        return container
    }

    func updateUIView(_ container: UIView, context: Context) {
        SharedRouteMapLibre.shared.attach(to: container)
    }

    static func dismantleUIView(_ uiView: UIView, coordinator: ()) {
        SharedRouteMapLibre.shared.park()
    }
}
