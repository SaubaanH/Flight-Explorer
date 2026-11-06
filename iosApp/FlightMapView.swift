import MapKit
import SwiftUI
import Shared

struct FlightMapView: View {
    @ObservedObject var model: FlightExplorerModel
    var onSelectFlight: (String) -> Void = { _ in }
    @State private var position: MapCameraPosition = .automatic

    var body: some View {
        Map(position: $position) {
            ForEach(model.clusters, id: \.destination) { cluster in
                Annotation(cluster.destination, coordinate: CLLocationCoordinate2D(
                    latitude: cluster.latitude,
                    longitude: cluster.longitude
                )) {
                    Button {
                        model.openCluster(cluster)
                    } label: {
                        VStack(spacing: 2) {
                            Text(cluster.destination).font(.caption.bold())
                            Text("\(cluster.flightCount) · \(cluster.lowestPoints.formatted())pts")
                                .font(.caption2)
                        }
                        .padding(6)
                        .background(RoameTheme.pink)
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .accessibilityLabel(
                        "\(cluster.destinationCity) \(cluster.flightCount) flights from \(cluster.lowestPoints) points"
                    )
                }
            }

            if let flight = model.selectedFlight {
                let geometry = RouteMapGeometry.route(for: flight)
                ForEach(Array(geometry.polylines.enumerated()), id: \.offset) { _, coords in
                    MapPolyline(coordinates: coords)
                        .stroke(RoameTheme.pink, lineWidth: 3)
                }

                Marker(flight.origin, coordinate: geometry.origin)
                Marker(flight.destination, coordinate: geometry.destination)
            }
        }
        .mapControls {
            MapCompass()
            MapPitchToggle()
        }
        .onChange(of: model.selectedFlight?.id) { _, _ in
            fitCamera()
        }
        .onAppear {
            fitCamera()
        }
        .sheet(item: Binding(
            get: { model.browsingCluster.map(ClusterSheetItem.init) },
            set: { if $0 == nil { model.dismissCluster() } }
        )) { item in
            NavigationStack {
                List(item.cluster.flights, id: \.id) { flight in
                    FlightRowView(
                        card: FlightCard(flight),
                        selected: model.selectedFlight?.id == flight.id,
                        saved: model.isSaved(flight.id),
                        onTap: {
                            onSelectFlight(flight.id)
                            model.dismissCluster()
                        },
                        onSave: { model.save(flight) },
                        onRemove: { model.removeSaved(flight.id) }
                    )
                    .listRowInsets(EdgeInsets())
                    .listRowSeparator(.hidden)
                    .padding(.vertical, 4)
                }
                .navigationTitle("\(item.cluster.destination) · \(item.cluster.flightCount)")
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Close") { model.dismissCluster() }
                    }
                }
            }
        }
    }

    private func fitCamera() {
        if let flight = model.selectedFlight {
            withAnimation(.easeInOut(duration: 0.35)) {
                position = .region(RouteMapGeometry.region(for: flight))
            }
        }
    }
}

private struct ClusterSheetItem: Identifiable {
    let cluster: DestinationCluster
    var id: String { cluster.destination }
}
