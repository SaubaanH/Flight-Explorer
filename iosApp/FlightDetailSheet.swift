import MapKit
import SwiftUI
import Shared

struct SelectedFlightItem: Identifiable {
    let flight: Flight
    var id: String { flight.id }
}

/// Bottom sheet for a selected flight: details + embedded route map + Save/Remove.
/// Height follows content — no empty stretch below the action button.
struct FlightDetailSheet: View {
    let flight: Flight
    let saved: Bool
    let onSave: () -> Void
    let onRemove: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 4) {
                Text("\(flight.origin) → \(flight.destination)")
                    .font(.system(.title2, design: .serif).weight(.semibold))
                    .foregroundStyle(RoameTheme.ink)
                Text("\(flight.originCity) → \(flight.destinationCity)")
                    .font(.subheadline)
                    .foregroundStyle(RoameTheme.muted)
            }

            RoutePreviewMap(flight: flight)
                .frame(height: 180)
                .background(RoameTheme.canvas)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(RoameTheme.hairline, lineWidth: 1)
                )

            detailRow("Departure", flight.departureDate)
            detailRow("Cabin", flight.cabin)
            detailRow("Program", flight.program)

            Text("\(flight.points.formatted()) points")
                .font(.title2.bold())
                .foregroundStyle(RoameTheme.ink)

            if saved {
                RoamePillButton(label: "Remove", filled: false, action: onRemove)
                    .accessibilityLabel("Remove saved flight")
            } else {
                RoamePillButton(label: "Save", action: onSave)
                    .accessibilityLabel("Save flight")
            }
        }
        .padding(16)
        .padding(.bottom, 8)
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .presentationDetents([.height(520)])
        .presentationDragIndicator(.visible)
        .presentationBackground(RoameTheme.canvas)
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Selected flight details")
    }

    private func detailRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label)
                .font(.subheadline)
                .foregroundStyle(RoameTheme.muted)
            Spacer()
            Text(value)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(RoameTheme.ink)
        }
    }
}

private struct RoutePreviewMap: View {
    let flight: Flight

    private var origin: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: flight.originLatitude, longitude: flight.originLongitude)
    }

    private var destination: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: flight.destinationLatitude, longitude: flight.destinationLongitude)
    }

    private var camera: MapCameraPosition {
        let minLat = min(origin.latitude, destination.latitude)
        let maxLat = max(origin.latitude, destination.latitude)
        let minLng = min(origin.longitude, destination.longitude)
        let maxLng = max(origin.longitude, destination.longitude)
        let center = CLLocationCoordinate2D(
            latitude: (minLat + maxLat) / 2,
            longitude: (minLng + maxLng) / 2
        )
        let latDelta = max((maxLat - minLat) * 1.6, 8)
        let lngDelta = max((maxLng - minLng) * 1.6, 8)
        return .region(MKCoordinateRegion(
            center: center,
            span: MKCoordinateSpan(latitudeDelta: latDelta, longitudeDelta: lngDelta)
        ))
    }

    var body: some View {
        Map(initialPosition: camera) {
            Marker(flight.origin, coordinate: origin)
            Marker(flight.destination, coordinate: destination)
            MapPolyline(coordinates: [origin, destination])
                .stroke(RoameTheme.pink, lineWidth: 3)
        }
        .mapStyle(.standard(elevation: .flat))
        .disabled(true)
    }
}
