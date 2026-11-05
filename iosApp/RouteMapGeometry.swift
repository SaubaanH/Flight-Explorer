import MapKit
import Shared

/// Camera + polyline that take the short path across the antimeridian (e.g. SEA → SYD).
enum RouteMapGeometry {
    struct Route {
        let origin: CLLocationCoordinate2D
        let destination: CLLocationCoordinate2D
        /// One or more segments in [-180, 180]. Densified along the short great-circle arc.
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

        // Unwrapped destination so interpolation stays on the short arc.
        let unwrappedDest = CLLocationCoordinate2D(
            latitude: destinationLat,
            longitude: originLng + delta
        )
        let arc = densifyShortArc(from: origin, toUnwrapped: unwrappedDest, segments: 48)
        let polylines = splitCrossingAntimeridian(arc)

        return Route(
            origin: origin,
            destination: destination,
            polylines: polylines,
            region: regionFittingShortArc(origin: origin, unwrappedDest: unwrappedDest)
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

    // MARK: - Arc

    /// Linear interpolation in lat / unwrapped-lon space (short path). Enough points to read as a flight arc.
    private static func densifyShortArc(
        from origin: CLLocationCoordinate2D,
        toUnwrapped dest: CLLocationCoordinate2D,
        segments: Int
    ) -> [CLLocationCoordinate2D] {
        let n = max(segments, 2)
        return (0...n).map { i in
            let t = Double(i) / Double(n)
            return CLLocationCoordinate2D(
                latitude: origin.latitude + (dest.latitude - origin.latitude) * t,
                longitude: origin.longitude + (dest.longitude - origin.longitude) * t
            )
        }
    }

    /// Normalize each point into [-180, 180] and split into separate polylines at the dateline.
    private static func splitCrossingAntimeridian(
        _ points: [CLLocationCoordinate2D]
    ) -> [[CLLocationCoordinate2D]] {
        guard var previous = points.first else { return [] }
        previous = normalized(previous)

        var segments: [[CLLocationCoordinate2D]] = [[previous]]
        for raw in points.dropFirst() {
            let point = normalized(raw)
            let jump = abs(point.longitude - previous.longitude)
            if jump > 180 {
                // Dateline crossing — start a new segment.
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

    // MARK: - Camera

    /// Fit the short arc with padding. Uses unwrapped longitudes so Pacific routes
    /// don't expand to the long way through Africa. Pre-expands for the wide preview.
    private static func regionFittingShortArc(
        origin: CLLocationCoordinate2D,
        unwrappedDest: CLLocationCoordinate2D
    ) -> MKCoordinateRegion {
        let routeLat = abs(unwrappedDest.latitude - origin.latitude)
        let routeLng = abs(unwrappedDest.longitude - origin.longitude)

        // ~24% inset each side so endpoints aren't glued to the crop.
        var latDelta = max(routeLat * 1.48, 14)
        var lngDelta = max(routeLng * 1.48, 18)

        // Detail preview is wide (~2.15:1). If the span is wider than that, MapKit
        // expands latitude — pre-match so longitude padding isn't sacrificed.
        let viewAspect = 2.15
        if lngDelta / latDelta > viewAspect {
            latDelta = lngDelta / viewAspect
        } else {
            lngDelta = latDelta * viewAspect
        }

        latDelta = min(latDelta, 110)
        lngDelta = min(lngDelta, 175)

        var centerLon = (origin.longitude + unwrappedDest.longitude) / 2
        while centerLon > 180 { centerLon -= 360 }
        while centerLon < -180 { centerLon += 360 }

        return MKCoordinateRegion(
            center: CLLocationCoordinate2D(
                latitude: (origin.latitude + unwrappedDest.latitude) / 2,
                longitude: centerLon
            ),
            span: MKCoordinateSpan(latitudeDelta: latDelta, longitudeDelta: lngDelta)
        )
    }
}
