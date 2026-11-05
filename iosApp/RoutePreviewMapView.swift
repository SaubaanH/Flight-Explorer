import MapKit
import SwiftUI
import Shared

/// Detail-sheet route preview using MKMapView so we can apply *pixel* edge padding.
/// SwiftUI `Map` + degree spans can't keep long-haul endpoints inset (e.g. ORD → DOH).
struct RoutePreviewMapView: UIViewRepresentable {
    let flight: Flight

    func makeCoordinator() -> Coordinator {
        Coordinator()
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
        let geometry = RouteMapGeometry.route(for: flight)
        context.coordinator.geometry = geometry
        context.coordinator.flightId = flight.id

        mapView.removeOverlays(mapView.overlays)
        mapView.removeAnnotations(mapView.annotations)

        // Geodesic overlay — MapKit takes the short path (incl. Pacific routes).
        let coords = [geometry.origin, geometry.destination]
        mapView.addOverlay(MKGeodesicPolyline(coordinates: coords, count: 2), level: .aboveRoads)

        let originPin = EndpointAnnotation(coordinate: geometry.origin, title: flight.origin)
        let destPin = EndpointAnnotation(coordinate: geometry.destination, title: flight.destination)
        mapView.addAnnotations([originPin, destPin])

        context.coordinator.fit(mapView, force: true)
    }

    final class Coordinator: NSObject, MKMapViewDelegate {
        var geometry: RouteMapGeometry.Route?
        var flightId: String?
        private var lastFittedId: String?
        private var lastSize: CGSize = .zero

        func fit(_ mapView: MKMapView, force: Bool) {
            guard let geometry else { return }
            let size = mapView.bounds.size
            guard size.width > 1, size.height > 1 else {
                // Layout not ready — try again next run loop.
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

            // Pixel insets — works for long-haul where degree padding hits the ~180° ceiling.
            let inset = UIEdgeInsets(top: 30, left: 44, bottom: 30, right: 44)
            mapView.setVisibleMapRect(geometry.mapRect, edgePadding: inset, animated: false)
        }

        func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            if let polyline = overlay as? MKPolyline {
                let renderer = MKPolylineRenderer(polyline: polyline)
                renderer.strokeColor = UIColor(RoameTheme.pink)
                renderer.lineWidth = 3
                renderer.lineCap = .round
                renderer.lineJoin = .round
                return renderer
            }
            return MKOverlayRenderer(overlay: overlay)
        }

        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            guard annotation is EndpointAnnotation else { return nil }
            let id = "route-endpoint"
            let view = mapView.dequeueReusableAnnotationView(withIdentifier: id)
                ?? MKAnnotationView(annotation: annotation, reuseIdentifier: id)
            view.annotation = annotation
            view.canShowCallout = false
            view.image = Self.endpointDotImage
            view.centerOffset = .zero
            return view
        }

        func mapViewDidFinishLoadingMap(_ mapView: MKMapView) {
            fit(mapView, force: true)
        }

        func mapView(_ mapView: MKMapView, regionDidChangeAnimated animated: Bool) {
            // Re-apply if MapKit adjusts region during layout.
            if flightId != lastFittedId || mapView.bounds.size != lastSize {
                fit(mapView, force: true)
            }
        }

        private static let endpointDotImage: UIImage = {
            let size = CGSize(width: 14, height: 14)
            let renderer = UIGraphicsImageRenderer(size: size)
            return renderer.image { ctx in
                let rect = CGRect(origin: .zero, size: size).insetBy(dx: 1, dy: 1)
                UIColor.white.setFill()
                ctx.cgContext.fillEllipse(in: rect)
                UIColor(RoameTheme.pink).setFill()
                ctx.cgContext.fillEllipse(in: rect.insetBy(dx: 2.5, dy: 2.5))
            }
        }()
    }
}

private final class EndpointAnnotation: NSObject, MKAnnotation {
    dynamic var coordinate: CLLocationCoordinate2D
    let title: String?

    init(coordinate: CLLocationCoordinate2D, title: String) {
        self.coordinate = coordinate
        self.title = title
    }
}

/// Refits the short-path rect whenever Auto Layout gives the map a real size.
final class FittingMapView: MKMapView {
    var onLayout: ((MKMapView) -> Void)?

    override func layoutSubviews() {
        super.layoutSubviews()
        onLayout?(self)
    }
}
