import MapKit
import Shared

/// Short-path route geometry in continuous map-point space.
/// Critical: camera, line, and endpoints must share the *same* world copy.
/// Normalizing lon into [-180, 180] while fitting an unwrapped rect puts pins
/// on the opposite copy so both airports fall out of frame (e.g. JFK → HKG).
enum RouteMapGeometry {
    struct Route {
        let origin: CLLocationCoordinate2D
        let destination: CLLocationCoordinate2D
        /// Projected points along the short arc (x may be outside the primary world).
        let mapPoints: [MKMapPoint]
        let mapRect: MKMapRect
        /// Region center for fallback `setRegion` (normalized lon).
        let centerCoordinate: CLLocationCoordinate2D
        /// Normalized [-180, 180] segments for SwiftUI `MapPolyline` on Discover.
        let polylines: [[CLLocationCoordinate2D]]
    }

    static func route(for flight: Flight) -> Route {
        let origin = CLLocationCoordinate2D(
            latitude: flight.originLatitude,
            longitude: flight.originLongitude
        )
        let destination = CLLocationCoordinate2D(
            latitude: flight.destinationLatitude,
            longitude: flight.destinationLongitude
        )

        var delta = flight.destinationLongitude - flight.originLongitude
        while delta > 180 { delta -= 360 }
        while delta < -180 { delta += 360 }

        let unwrappedDestLon = flight.originLongitude + delta
        let n = 64
        var mapPoints: [MKMapPoint] = []
        var unwrappedCoords: [CLLocationCoordinate2D] = []
        mapPoints.reserveCapacity(n + 1)
        unwrappedCoords.reserveCapacity(n + 1)
        for i in 0...n {
            let t = Double(i) / Double(n)
            let lat = origin.latitude + (destination.latitude - origin.latitude) * t
            let lon = origin.longitude + (unwrappedDestLon - origin.longitude) * t
            unwrappedCoords.append(CLLocationCoordinate2D(latitude: lat, longitude: lon))
            mapPoints.append(mapPoint(latitude: lat, longitude: lon))
        }

        var centerLon = (origin.longitude + unwrappedDestLon) / 2
        while centerLon > 180 { centerLon -= 360 }
        while centerLon < -180 { centerLon += 360 }

        return Route(
            origin: origin,
            destination: destination,
            mapPoints: mapPoints,
            mapRect: paddedRect(covering: mapPoints, midLat: (origin.latitude + destination.latitude) / 2),
            centerCoordinate: CLLocationCoordinate2D(
                latitude: (origin.latitude + destination.latitude) / 2,
                longitude: centerLon
            ),
            polylines: splitCrossingAntimeridian(unwrappedCoords)
        )
    }

    private static func splitCrossingAntimeridian(
        _ points: [CLLocationCoordinate2D]
    ) -> [[CLLocationCoordinate2D]] {
        guard var previous = points.first else { return [] }
        previous = normalized(previous)

        var segments: [[CLLocationCoordinate2D]] = [[previous]]
        for raw in points.dropFirst() {
            let point = normalized(raw)
            if abs(point.longitude - previous.longitude) > 180 {
                segments.append([point])
            } else {
                segments[segments.count - 1].append(point)
            }
            previous = point
        }
        return segments.filter { $0.count >= 2 }
    }

    private static func normalized(_ c: CLLocationCoordinate2D) -> CLLocationCoordinate2D {
        var lon = c.longitude
        while lon > 180 { lon -= 360 }
        while lon < -180 { lon += 360 }
        return CLLocationCoordinate2D(latitude: c.latitude, longitude: lon)
    }

    /// Project lat/lon without clamping lon into [-180, 180] — needed for short Pacific arcs.
    static func mapPoint(latitude: Double, longitude: Double) -> MKMapPoint {
        let worldW = MKMapSize.world.width
        let x = (longitude + 180.0) / 360.0 * worldW
        let y = MKMapPoint(CLLocationCoordinate2D(latitude: latitude, longitude: 0)).y
        return MKMapPoint(x: x, y: y)
    }

    private static func paddedRect(covering points: [MKMapPoint], midLat: Double) -> MKMapRect {
        guard let first = points.first else { return .world }
        var minX = first.x, maxX = first.x, minY = first.y, maxY = first.y
        for p in points.dropFirst() {
            minX = min(minX, p.x); maxX = max(maxX, p.x)
            minY = min(minY, p.y); maxY = max(maxY, p.y)
        }

        let minPoints = 800_000 / max(MKMetersPerMapPointAtLatitude(midLat), 1)
        var width = max(maxX - minX, minPoints)
        var height = max(maxY - minY, minPoints)

        // Detail preview is ~2.15:1 — pre-expand so MapKit aspect fit doesn't crop the long axis.
        let viewAspect = 2.15
        if width / max(height, 1) > viewAspect {
            height = width / viewAspect
        } else {
            width = height * viewAspect
        }

        // Extra inset in map-point space (~18% each side) before pixel edgePadding.
        width *= 1.36
        height *= 1.36

        let cx = (minX + maxX) / 2
        let cy = (minY + maxY) / 2
        return MKMapRect(x: cx - width / 2, y: cy - height / 2, width: width, height: height)
    }
}

/// Draws the short-path line + endpoint dots in the same map-point space as `boundingMapRect`.
final class ShortPathRouteOverlay: NSObject, MKOverlay {
    let coordinate: CLLocationCoordinate2D
    let boundingMapRect: MKMapRect
    let mapPoints: [MKMapPoint]

    init(geometry: RouteMapGeometry.Route) {
        coordinate = geometry.centerCoordinate
        boundingMapRect = geometry.mapRect
        mapPoints = geometry.mapPoints
        super.init()
    }
}

final class ShortPathRouteRenderer: MKOverlayRenderer {
    private let routeOverlay: ShortPathRouteOverlay

    init(overlay: ShortPathRouteOverlay) {
        self.routeOverlay = overlay
        super.init(overlay: overlay)
    }

    override func draw(_ mapRect: MKMapRect, zoomScale: MKZoomScale, in context: CGContext) {
        let points = routeOverlay.mapPoints
        guard points.count >= 2 else { return }

        let path = CGMutablePath()
        path.move(to: self.point(for: points[0]))
        for p in points.dropFirst() {
            path.addLine(to: self.point(for: p))
        }

        context.saveGState()
        context.setStrokeColor(UIColor(RoameTheme.pink).cgColor)
        context.setLineWidth(max(3 / zoomScale, 1))
        context.setLineCap(.round)
        context.setLineJoin(.round)
        context.addPath(path)
        context.strokePath()

        let r = max(6 / zoomScale, 2)
        for end in [points.first!, points.last!] {
            let c = self.point(for: end)
            let outer = CGRect(x: c.x - r, y: c.y - r, width: r * 2, height: r * 2)
            context.setFillColor(UIColor(RoameTheme.pink).cgColor)
            context.fillEllipse(in: outer)
            let inset = r * 0.4
            context.setFillColor(UIColor.white.cgColor)
            context.fillEllipse(in: outer.insetBy(dx: inset, dy: inset))
        }
        context.restoreGState()
    }
}
