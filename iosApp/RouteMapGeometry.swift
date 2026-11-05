import MapKit
import Shared

/// Camera + polyline that take the short path across the antimeridian (e.g. SEA → SYD).
enum RouteMapGeometry {
    struct Route {
        let origin: CLLocationCoordinate2D
        let destination: CLLocationCoordinate2D
        /// One or two segments; split at ±180 when the short arc crosses the dateline.
        let polylines: [[CLLocationCoordinate2D]]
        let region: MKCoordinateRegion
    }

    static func route(
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double
    ) -> Route {
        let origin = CLLocationCoordinate2D(latitude: originLat, longitude: originLng)
        let destination = CLLocationCoordinate2D(latitude: destinationLat, longitude: destinationLng)

        var delta = destinationLng - originLng
        while delta > 180 { delta -= 360 }
        while delta < -180 { delta += 360 }

        let lineDestLon = originLng + delta
        let polylines: [[CLLocationCoordinate2D]]
        if lineDestLon >= -180, lineDestLon <= 180 {
            // Short path stays in a single longitude domain.
            polylines = [[origin, destination]]
        } else {
            // Crosses the antimeridian — split so every coordinate stays in [-180, 180].
            let edgeLon: Double = delta < 0 ? -180 : 180
            let t = (edgeLon - originLng) / delta
            let crossLat = originLat + t * (destinationLat - originLat)
            let otherLon: Double = delta < 0 ? 180 : -180
            polylines = [
                [origin, CLLocationCoordinate2D(latitude: crossLat, longitude: edgeLon)],
                [CLLocationCoordinate2D(latitude: crossLat, longitude: otherLon), destination]
            ]
        }

        var centerLon = originLng + delta / 2
        while centerLon > 180 { centerLon -= 360 }
        while centerLon < -180 { centerLon += 360 }

        let center = CLLocationCoordinate2D(
            latitude: (originLat + destinationLat) / 2,
            longitude: centerLon
        )
        let latDelta = min(max(abs(destinationLat - originLat) * 1.85, 24), 140)
        let lngDelta = min(max(abs(delta) * 1.85, 28), 160)

        return Route(
            origin: origin,
            destination: destination,
            polylines: polylines,
            region: MKCoordinateRegion(
                center: center,
                span: MKCoordinateSpan(latitudeDelta: latDelta, longitudeDelta: lngDelta)
            )
        )
    }

    static func route(for flight: Flight) -> Route {
        route(
            originLat: flight.originLatitude,
            originLng: flight.originLongitude,
            destinationLat: flight.destinationLatitude,
            destinationLng: flight.destinationLongitude
        )
    }
}
