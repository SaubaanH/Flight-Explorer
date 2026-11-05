import MapKit
import Shared

/// Short-path route geometry for MapKit.
/// Straight short-lon arc (not a polar great-circle) so Pacific routes frame over
/// the ocean instead of the Arctic.
enum RouteMapGeometry {
    struct Route {
        let origin: CLLocationCoordinate2D
        let destination: CLLocationCoordinate2D
        /// Normalized [-180, 180] segments for overlays.
        let polylines: [[CLLocationCoordinate2D]]
        /// Continuous projected rect for `setVisibleMapRect` (may extend past world edges).
        let mapRect: MKMapRect
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

        let unwrappedDest = CLLocationCoordinate2D(
            latitude: flight.destinationLatitude,
            longitude: flight.originLongitude + delta
        )

        let n = 48
        let arc: [CLLocationCoordinate2D] = (0...n).map { i in
            let t = Double(i) / Double(n)
            return CLLocationCoordinate2D(
                latitude: origin.latitude + (unwrappedDest.latitude - origin.latitude) * t,
                longitude: origin.longitude + (unwrappedDest.longitude - origin.longitude) * t
            )
        }

        return Route(
            origin: origin,
            destination: destination,
            polylines: splitCrossingAntimeridian(arc),
            mapRect: mapRect(coveringUnwrapped: arc)
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

    private static func mapRect(coveringUnwrapped points: [CLLocationCoordinate2D]) -> MKMapRect {
        let worldW = MKMapSize.world.width
        func mapX(_ lon: Double) -> Double { (lon + 180.0) / 360.0 * worldW }
        func mapY(_ lat: Double) -> Double {
            MKMapPoint(CLLocationCoordinate2D(latitude: lat, longitude: 0)).y
        }

        guard let first = points.first else { return .world }
        var minX = mapX(first.longitude)
        var maxX = minX
        var minY = mapY(first.latitude)
        var maxY = minY
        for p in points.dropFirst() {
            let x = mapX(p.longitude)
            let y = mapY(p.latitude)
            minX = min(minX, x)
            maxX = max(maxX, x)
            minY = min(minY, y)
            maxY = max(maxY, y)
        }

        let midLat = (points.first!.latitude + points.last!.latitude) / 2
        let minPoints = 800_000 / max(MKMetersPerMapPointAtLatitude(midLat), 1)
        let width = max(maxX - minX, minPoints)
        let height = max(maxY - minY, minPoints)
        let cx = (minX + maxX) / 2
        let cy = (minY + maxY) / 2
        return MKMapRect(x: cx - width / 2, y: cy - height / 2, width: width, height: height)
    }
}
