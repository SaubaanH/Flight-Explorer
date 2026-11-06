import MapKit
import SwiftUI
import Shared

struct FlightMapView: View {
    @ObservedObject var model: FlightExplorerModel
    @State private var position: MapCameraPosition = .automatic
    @State private var lastRouteSignature: String = ""

    var body: some View {
        Map(position: $position) {
            ForEach(model.clusters, id: \.destination) { cluster in
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
                        .background(
                            model.mapFocusAirport == cluster.destination
                                ? RoameTheme.ink
                                : RoameTheme.pink
                        )
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .accessibilityLabel(
                        "\(cluster.destinationCity) \(cluster.flightCount) flights from \(cluster.lowestPoints) points"
                    )
                }
            }

            // Selected flight → one route; map focus with no selection → all focused routes.
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
        .onChange(of: routeSignature) { _, newValue in
            fitCamera(signature: newValue)
        }
        .onAppear {
            fitCamera(signature: routeSignature)
        }
    }

    private var routeItems: [MapRouteItem] {
        model.mapRouteFlights.map(MapRouteItem.init)
    }

    private var routeSignature: String {
        routeItems.map(\.id).joined(separator: "|")
    }

    private func fitCamera(signature: String) {
        guard signature != lastRouteSignature else { return }
        lastRouteSignature = signature
        let items = routeItems
        guard let first = items.first else { return }
        if items.count == 1, let flight = model.selectedFlight ?? model.flight(for: first.id) {
            withAnimation(.easeInOut(duration: 0.35)) {
                position = .region(RouteMapGeometry.region(for: flight))
            }
            return
        }
        var minLat = first.origin.latitude
        var maxLat = first.origin.latitude
        var minLng = first.origin.longitude
        var maxLng = first.origin.longitude
        for item in items {
            for coord in [item.origin, item.destination] + item.polylines.flatMap({ $0 }) {
                minLat = min(minLat, coord.latitude)
                maxLat = max(maxLat, coord.latitude)
                minLng = min(minLng, coord.longitude)
                maxLng = max(maxLng, coord.longitude)
            }
        }
        let region = MKCoordinateRegion(
            center: CLLocationCoordinate2D(
                latitude: (minLat + maxLat) / 2,
                longitude: (minLng + maxLng) / 2
            ),
            span: MKCoordinateSpan(
                latitudeDelta: min(max((maxLat - minLat) * 1.4, 12), 100),
                longitudeDelta: min(max((maxLng - minLng) * 1.4, 12), 140)
            )
        )
        withAnimation(.easeInOut(duration: 0.35)) {
            position = .region(region)
        }
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
