import MapKit
import SwiftUI
import Shared

struct FlightMapView: View {
    @ObservedObject var model: FlightExplorerModel
    @State private var position: MapCameraPosition = .automatic

    var body: some View {
        Map(position: $position) {
            ForEach(model.clusters, id: \.destination) { cluster in
                let emphasis = markerEmphasis(for: cluster.destination)
                Annotation(cluster.destination, coordinate: CLLocationCoordinate2D(
                    latitude: cluster.latitude,
                    longitude: cluster.longitude
                )) {
                    Button {
                        model.toggleMapFocus(cluster.destination)
                    } label: {
                        VStack(spacing: 2) {
                            Text(cluster.destination).font(.caption.bold())
                            Text("\(cluster.flightCount) · \(cluster.lowestPoints.formatted())pts")
                                .font(.caption2)
                        }
                        .padding(6)
                        .background(emphasis.background)
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .opacity(emphasis.opacity)
                    }
                    .accessibilityLabel(
                        "\(cluster.destinationCity) \(cluster.flightCount) flights from \(cluster.lowestPoints) points"
                    )
                }
            }

            ForEach(routeItems) { item in
                ForEach(Array(item.polylines.enumerated()), id: \.offset) { _, coords in
                    MapPolyline(coordinates: coords)
                        .stroke(RoameTheme.pink, lineWidth: 3)
                }
            }

            if let item = routeItems.first, model.selectedFlight != nil {
                Marker(item.originLabel, coordinate: item.origin)
                Marker(item.destinationLabel, coordinate: item.destination)
            }
        }
        .mapControls {
            MapCompass()
            MapPitchToggle()
        }
        .onChange(of: model.selectedFlight?.id) { _, _ in
            frameRoutes(animated: true)
        }
        .onChange(of: model.mapFocusAirport) { _, _ in
            frameRoutes(animated: true)
        }
        .onChange(of: routeSignature) { _, _ in
            frameRoutes(animated: true)
        }
        .onAppear {
            frameRoutes(animated: false)
        }
    }

    private var routeItems: [MapRouteItem] {
        model.mapRouteFlights.map(MapRouteItem.init)
    }

    private var routeSignature: String {
        routeItems.map(\.id).joined(separator: "|")
    }

    /// Airports that stay vivid; everything else is grayed when a route/focus is active.
    private var emphasizedAirports: Set<String> {
        if let flight = model.selectedFlight {
            return [flight.origin.uppercased(), flight.destination.uppercased()]
        }
        if let focus = model.mapFocusAirport {
            return [focus.uppercased()]
        }
        return []
    }

    private func markerEmphasis(for code: String) -> (background: Color, opacity: Double) {
        let emphasized = emphasizedAirports
        guard !emphasized.isEmpty else {
            return (RoameTheme.pink, 1)
        }
        if emphasized.contains(code.uppercased()) {
            let bg = model.mapFocusAirport?.uppercased() == code.uppercased() && model.selectedFlight == nil
                ? RoameTheme.ink
                : RoameTheme.pink
            return (bg, 1)
        }
        return (Color(white: 0.62), 0.45)
    }

    private func frameRoutes(animated: Bool) {
        let items = routeItems
        guard !items.isEmpty else { return }

        let region: MKCoordinateRegion
        if items.count == 1, let flight = model.selectedFlight ?? model.flight(for: items[0].id) {
            region = RouteMapGeometry.region(for: flight)
        } else {
            region = Self.unionRegion(items: items)
        }

        // Reset first so SwiftUI Map actually applies a new camera (same-region sets were no-ops).
        let apply = {
            position = .region(region)
        }
        if animated {
            position = .automatic
            DispatchQueue.main.async {
                withAnimation(.easeInOut(duration: 0.4)) {
                    apply()
                }
            }
        } else {
            apply()
        }
    }

    private static func unionRegion(items: [MapRouteItem]) -> MKCoordinateRegion {
        var minLat = items[0].origin.latitude
        var maxLat = items[0].origin.latitude
        var minLng = items[0].origin.longitude
        var maxLng = items[0].origin.longitude
        for item in items {
            for coord in [item.origin, item.destination] + item.polylines.flatMap({ $0 }) {
                minLat = min(minLat, coord.latitude)
                maxLat = max(maxLat, coord.latitude)
                minLng = min(minLng, coord.longitude)
                maxLng = max(maxLng, coord.longitude)
            }
        }
        return MKCoordinateRegion(
            center: CLLocationCoordinate2D(
                latitude: (minLat + maxLat) / 2,
                longitude: (minLng + maxLng) / 2
            ),
            span: MKCoordinateSpan(
                latitudeDelta: min(max((maxLat - minLat) * 1.55, 12), 100),
                longitudeDelta: min(max((maxLng - minLng) * 1.55, 12), 140)
            )
        )
    }
}

struct MapRouteItem: Identifiable, Equatable {
    let id: String
    let originLabel: String
    let destinationLabel: String
    let origin: CLLocationCoordinate2D
    let destination: CLLocationCoordinate2D
    let polylines: [[CLLocationCoordinate2D]]

    init(_ flight: Flight) {
        let geometry = RouteMapGeometry.route(for: flight)
        id = flight.id
        originLabel = flight.origin
        destinationLabel = flight.destination
        origin = geometry.origin
        destination = geometry.destination
        polylines = geometry.polylines
    }

    static func == (lhs: MapRouteItem, rhs: MapRouteItem) -> Bool {
        lhs.id == rhs.id
    }
}
