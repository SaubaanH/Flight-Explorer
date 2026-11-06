import MapKit
import Shared

/// Short-path polylines for the Discover MapKit map.
/// Detail sheet uses MapLibre (see `RoutePreviewMapView`) — MapKit cannot keep
/// both airports in frame across the dateline without world-copy mismatches.
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

    /// Camera region along the short-path arc (handles Pacific / dateline spans).
    static func region(for flight: Flight) -> MKCoordinateRegion {
        var delta = flight.destinationLongitude - flight.originLongitude
        while delta > 180 { delta -= 360 }
        while delta < -180 { delta += 360 }

        let midLat = (flight.originLatitude + flight.destinationLatitude) / 2
        var midLng = flight.originLongitude + delta / 2
        while midLng > 180 { midLng -= 360 }
        while midLng < -180 { midLng += 360 }

        let latDelta = min(max(abs(flight.destinationLatitude - flight.originLatitude) * 1.55, 12), 100)
        let lngDelta = min(max(abs(delta) * 1.55, 12), 140)
        return MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: midLat, longitude: midLng),
            span: MKCoordinateSpan(latitudeDelta: latDelta, longitudeDelta: lngDelta)
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
