import MapKit
import SwiftUI
import Shared

struct FlightMapView: View {
    @ObservedObject var model: FlightExplorerModel

    var body: some View {
        Map {
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
                        .background(Color.green.opacity(0.9))
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .accessibilityLabel(
                        "\(cluster.destinationCity) \(cluster.flightCount) flights from \(cluster.lowestPoints) points"
                    )
                }
            }

            if let flight = model.selectedFlight {
                MapPolyline(coordinates: [
                    CLLocationCoordinate2D(latitude: flight.originLatitude, longitude: flight.originLongitude),
                    CLLocationCoordinate2D(latitude: flight.destinationLatitude, longitude: flight.destinationLongitude),
                ])
                .stroke(Color.green, lineWidth: 3)

                Marker(flight.origin, coordinate: CLLocationCoordinate2D(
                    latitude: flight.originLatitude,
                    longitude: flight.originLongitude
                ))
            }
        }
        .mapControls {
            MapCompass()
            MapPitchToggle()
        }
        .sheet(item: Binding(
            get: { model.browsingCluster.map(ClusterSheetItem.init) },
            set: { if $0 == nil { model.dismissCluster() } }
        )) { item in
            NavigationStack {
                List(item.cluster.flights as? [Flight] ?? [], id: \.id) { flight in
                    FlightRowView(
                        flight: flight,
                        selected: model.selectedFlight?.id == flight.id,
                        saved: model.isSaved(flight.id),
                        onTap: {
                            model.select(flight.id)
                            model.dismissCluster()
                        }
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
}

private struct ClusterSheetItem: Identifiable {
    let cluster: DestinationCluster
    var id: String { cluster.destination }
}
