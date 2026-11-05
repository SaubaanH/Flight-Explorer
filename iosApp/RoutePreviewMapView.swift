import SwiftUI
import Shared

/// Instant detail-sheet route sketch (no WebView / tile fetch).
/// Same short-path framing as Android `RoutePreview`.
struct RoutePreviewMapView: View {
    let flight: Flight

    var body: some View {
        ZStack {
            Color(red: 0.94, green: 0.93, blue: 0.92)

            Canvas { context, size in
                let geometry = ShortPathLayout(flight: flight, size: size, inset: 28)
                var glow = Path()
                glow.move(to: geometry.origin)
                glow.addLine(to: geometry.destination)
                context.stroke(
                    glow,
                    with: .color(RoameTheme.pink.opacity(0.3)),
                    style: StrokeStyle(lineWidth: 6, lineCap: .round, dash: [10, 8])
                )
                var line = Path()
                line.move(to: geometry.origin)
                line.addLine(to: geometry.destination)
                context.stroke(
                    line,
                    with: .color(RoameTheme.pink),
                    style: StrokeStyle(lineWidth: 3, lineCap: .round)
                )
                drawEndpoint(context, at: geometry.origin)
                drawEndpoint(context, at: geometry.destination)
            }
            .padding(4)

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
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Route from \(flight.origin) to \(flight.destination)")
    }

    private func drawEndpoint(_ context: GraphicsContext, at point: CGPoint) {
        let outer = CGRect(x: point.x - 6, y: point.y - 6, width: 12, height: 12)
        context.fill(Path(ellipseIn: outer), with: .color(RoameTheme.pink))
        let inner = outer.insetBy(dx: 3.5, dy: 3.5)
        context.fill(Path(ellipseIn: inner), with: .color(.white))
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
            let x = draw.minX + CGFloat((lng - left) / spanLng) * draw.width
            let y = draw.minY + CGFloat((top - lat) / spanLat) * draw.height
            return CGPoint(x: x, y: y)
        }

        origin = project(lat: flight.originLatitude, lng: flight.originLongitude)
        destination = project(lat: flight.destinationLatitude, lng: destLng)
    }
}
