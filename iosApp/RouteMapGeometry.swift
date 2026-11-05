import MapKit
import Shared

/// Short-path polylines for the Discover MapKit map (not the detail preview).
/// Uses a straight short-lon arc — not a polar great-circle — so Pacific routes
/// stay framed over the ocean instead of the Arctic.
enum RouteMapGeometry {
    struct Route {
        let origin: CLLocationCoordinate2D
        let destination: CLLocationCoordinate2D
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

        let unwrappedDest = CLLocationCoordinate2D(
            latitude: flight.destinationLatitude,
            longitude: flight.originLongitude + delta
        )

        let n = 32
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
            polylines: splitCrossingAntimeridian(arc)
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
}
