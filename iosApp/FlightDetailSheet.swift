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
                .id(flight.id)
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
    @State private var cameraPosition: MapCameraPosition

    init(flight: Flight) {
        self.flight = flight
        let geometry = RouteMapGeometry.route(for: flight)
        _cameraPosition = State(initialValue: .region(geometry.region))
    }

    private var geometry: RouteMapGeometry.Route {
        RouteMapGeometry.route(for: flight)
    }

    var body: some View {
        // Explicit camera binding keeps MapKit from reframing to the long-way
        // bounds of origin/destination when the short path crosses the dateline.
        Map(position: $cameraPosition) {
            Marker(flight.origin, coordinate: geometry.origin)
            Marker(flight.destination, coordinate: geometry.destination)
            ForEach(Array(geometry.polylines.enumerated()), id: \.offset) { _, coords in
                MapPolyline(coordinates: coords)
                    .stroke(RoameTheme.pink, lineWidth: 3)
            }
        }
        .mapStyle(.standard(elevation: .flat, pointsOfInterest: .excludingAll))
        .disabled(true)
        .mapControlVisibility(.hidden)
        .onAppear {
            cameraPosition = .region(geometry.region)
        }
    }
}
