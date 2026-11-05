import MapKit
import Shared

/// Short-path route geometry for MapKit (handles Pacific crossings like SEA → SYD).
enum RouteMapGeometry {
    struct Route {
        let origin: CLLocationCoordinate2D
        let destination: CLLocationCoordinate2D
        /// Dest longitude shifted by ±360 when needed so the short arc is continuous.
        let unwrappedDestination: CLLocationCoordinate2D
        /// Normalized [-180, 180] segments for SwiftUI `MapPolyline` fallbacks.
        let polylines: [[CLLocationCoordinate2D]]
        /// Projected rect for `setVisibleMapRect` (may extend past world edges for dateline routes).
        let mapRect: MKMapRect
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

        let unwrappedDest = CLLocationCoordinate2D(
            latitude: destinationLat,
            longitude: originLng + delta
        )

        // Prefer geodesic samples so northern/southern bulges stay inside the frame.
        let arc = geodesicUnwrapped(from: origin, to: destination, unwrappedDest: unwrappedDest, samples: 64)
        let polylines = splitCrossingAntimeridian(arc)
        let mapRect = mapRect(coveringUnwrapped: arc)

        return Route(
            origin: origin,
            destination: destination,
            unwrappedDestination: unwrappedDest,
            polylines: polylines,
            mapRect: mapRect
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

    /// Great-circle samples with longitude kept continuous along the short path.
    private static func geodesicUnwrapped(
        from origin: CLLocationCoordinate2D,
        to destination: CLLocationCoordinate2D,
        unwrappedDest: CLLocationCoordinate2D,
        samples: Int
    ) -> [CLLocationCoordinate2D] {
        let geodesic = MKGeodesicPolyline(coordinates: [origin, destination], count: 2)
        let count = Int(geodesic.pointCount)
        guard count >= 2 else {
            return densifyLinear(from: origin, toUnwrapped: unwrappedDest, segments: samples)
        }

        var points: [CLLocationCoordinate2D] = []
        points.reserveCapacity(count)
        var prevUnwrappedLon = origin.longitude
        let ptr = geodesic.points()
        for i in 0..<count {
            let raw = ptr[i].coordinate
            var lon = raw.longitude
            // Unwrap onto the same continuous axis as the short-path destination.
            while lon - prevUnwrappedLon > 180 { lon -= 360 }
            while lon - prevUnwrappedLon < -180 { lon += 360 }
            // If geodesic went the long way (rare), snap toward unwrappedDest side.
            let toShort = unwrappedDest.longitude - origin.longitude
            let toHere = lon - origin.longitude
            if toShort * toHere < 0, abs(toHere) > 90 {
                lon += toHere > 0 ? -360 : 360
            }
            prevUnwrappedLon = lon
            points.append(CLLocationCoordinate2D(latitude: raw.latitude, longitude: lon))
        }

        // Ensure final point matches unwrapped destination longitude domain.
        if let last = points.last {
            var endLon = destination.longitude
            while endLon - last.longitude > 180 { endLon -= 360 }
            while endLon - last.longitude < -180 { endLon += 360 }
            // Prefer the precomputed short-path unwrap if still ambiguous.
            if abs(endLon - unwrappedDest.longitude) > abs(unwrappedDest.longitude - last.longitude) {
                endLon = unwrappedDest.longitude
            }
            points[points.count - 1] = CLLocationCoordinate2D(
                latitude: destination.latitude,
                longitude: endLon
            )
        }
        return points
    }

    private static func densifyLinear(
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

    // MARK: - Map rect

    /// Build an `MKMapRect` in continuous projection space (x may be <0 or >world width).
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

        // Minimum size ≈ 800km so short hops aren't over-zoomed.
        let midLat = (points.first!.latitude + points.last!.latitude) / 2
        let minMeters: Double = 800_000
        let minPoints = minMeters / max(MKMetersPerMapPointAtLatitude(midLat), 1)
        let width = max(maxX - minX, minPoints)
        let height = max(maxY - minY, minPoints)
        let cx = (minX + maxX) / 2
        let cy = (minY + maxY) / 2
        return MKMapRect(x: cx - width / 2, y: cy - height / 2, width: width, height: height)
    }
}
