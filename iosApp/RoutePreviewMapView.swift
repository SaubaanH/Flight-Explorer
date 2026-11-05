import MapKit
import SwiftUI
import Shared

/// Real MapKit preview. Line + endpoints are one overlay in unwrapped map-point
/// space so both airports stay in frame on antimeridian routes.
struct RoutePreviewMapView: View {
    let flight: Flight
    @State private var mapReady = false

    var body: some View {
        ZStack {
            RouteSketchView(flight: flight)
                .opacity(mapReady ? 0 : 1)

            RouteMapKitView(flight: flight) {
                withAnimation(.easeOut(duration: 0.25)) { mapReady = true }
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

// MARK: - MapKit

private struct RouteMapKitView: UIViewRepresentable {
    let flight: Flight
    var onReady: () -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onReady: onReady)
    }

    func makeUIView(context: Context) -> FittingMapView {
        let map = FittingMapView(frame: .zero)
        map.delegate = context.coordinator
        map.onLayout = { [weak coordinator = context.coordinator] map in
            coordinator?.fit(map, force: false)
        }
        map.isScrollEnabled = false
        map.isZoomEnabled = false
        map.isRotateEnabled = false
        map.isPitchEnabled = false
        map.isUserInteractionEnabled = false
        map.showsCompass = false
        map.pointOfInterestFilter = .excludingAll
        map.overrideUserInterfaceStyle = .light
        return map
    }

    func updateUIView(_ mapView: FittingMapView, context: Context) {
        context.coordinator.onReady = onReady
        let geometry = RouteMapGeometry.route(for: flight)
        context.coordinator.geometry = geometry
        context.coordinator.flightId = flight.id

        mapView.removeOverlays(mapView.overlays)
        mapView.addOverlay(ShortPathRouteOverlay(geometry: geometry), level: .aboveRoads)
        context.coordinator.fit(mapView, force: true)
    }

    final class Coordinator: NSObject, MKMapViewDelegate {
        var geometry: RouteMapGeometry.Route?
        var flightId: String?
        var onReady: () -> Void
        private var lastFittedId: String?
        private var lastSize: CGSize = .zero
        private var didSignalReady = false

        init(onReady: @escaping () -> Void) {
            self.onReady = onReady
        }

        func fit(_ mapView: MKMapView, force: Bool) {
            guard let geometry else { return }
            let size = mapView.bounds.size
            guard size.width > 1, size.height > 1 else {
                DispatchQueue.main.async { [weak self, weak mapView] in
                    guard let self, let mapView else { return }
                    self.fit(mapView, force: true)
                }
                return
            }

            let sizeChanged = abs(size.width - lastSize.width) > 1 || abs(size.height - lastSize.height) > 1
            let idChanged = flightId != lastFittedId
            guard force || sizeChanged || idChanged else { return }
            lastSize = size
            lastFittedId = flightId
            if idChanged { didSignalReady = false }

            // Pixel padding on top of the already-padded mapRect.
            let inset = UIEdgeInsets(top: 24, left: 36, bottom: 24, right: 36)
            mapView.setVisibleMapRect(geometry.mapRect, edgePadding: inset, animated: false)
        }

        func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            if let route = overlay as? ShortPathRouteOverlay {
                return ShortPathRouteRenderer(overlay: route)
            }
            return MKOverlayRenderer(overlay: overlay)
        }

        func mapViewDidFinishLoadingMap(_ mapView: MKMapView) {
            fit(mapView, force: true)
            signalReady()
        }

        func mapViewDidFinishRenderingMap(_ mapView: MKMapView, fullyRendered: Bool) {
            if fullyRendered {
                fit(mapView, force: true)
                signalReady()
            }
        }

        private func signalReady() {
            guard !didSignalReady else { return }
            didSignalReady = true
            onReady()
        }
    }
}

final class FittingMapView: MKMapView {
    var onLayout: ((MKMapView) -> Void)?

    override func layoutSubviews() {
        super.layoutSubviews()
        onLayout?(self)
    }
}
