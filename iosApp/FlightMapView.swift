import MapKit
import Shared
import SwiftUI

struct FlightMapView: View {
    let clusters: [DestinationCluster]
    let selectedFlight: Flight?
    let onClusterTap: (DestinationCluster) -> Void

    @State private var position: MapCameraPosition = .automatic

    var body: some View {
        Map(position: $position) {
            ForEach(clusters, id: \.destination) { cluster in
                Annotation(
                    "\(cluster.destination) · \(cluster.flightCount) · \(cluster.lowestPoints.formatted())pts",
                    coordinate: CLLocationCoordinate2D(
                        latitude: cluster.latitude,
                        longitude: cluster.longitude
                    )
                ) {
                    Button {
                        onClusterTap(cluster)
                    } label: {
                        Text("\(cluster.destination) · \(cluster.flightCount) · \(cluster.lowestPoints.formatted())pts")
                            .font(.caption2.weight(.semibold))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color(red: 0.04, green: 0.43, blue: 0.31))
                            .foregroundStyle(.white)
                            .clipShape(Capsule())
                    }
                    .accessibilityLabel(
                        "\(cluster.destinationCity) \(cluster.flightCount) flights from \(cluster.lowestPoints) points"
                    )
                }
            }

            if let flight = selectedFlight {
                MapPolyline(
                    coordinates: [
                        CLLocationCoordinate2D(
                            latitude: flight.originLatitude,
                            longitude: flight.originLongitude
                        ),
                        CLLocationCoordinate2D(
                            latitude: flight.destinationLatitude,
                            longitude: flight.destinationLongitude
                        ),
                    ]
                )
                .stroke(Color(red: 0.04, green: 0.43, blue: 0.31), lineWidth: 3)
            }
        }
        .mapStyle(.standard)
        .onChange(of: clusters.map(\.destination)) { _, _ in
            fitCamera()
        }
        .onAppear { fitCamera() }
    }

    private func fitCamera() {
        var coords = clusters.map {
            CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude)
        }
        if let flight = selectedFlight {
            coords.append(
                CLLocationCoordinate2D(
                    latitude: flight.originLatitude,
                    longitude: flight.originLongitude
                )
            )
            coords.append(
                CLLocationCoordinate2D(
                    latitude: flight.destinationLatitude,
                    longitude: flight.destinationLongitude
                )
            )
        }
        guard !coords.isEmpty else { return }
        let latitudes = coords.map(\.latitude)
        let longitudes = coords.map(\.longitude)
        let center = CLLocationCoordinate2D(
            latitude: (latitudes.min()! + latitudes.max()!) / 2,
            longitude: (longitudes.min()! + longitudes.max()!) / 2
        )
        let span = MKCoordinateSpan(
            latitudeDelta: max((latitudes.max()! - latitudes.min()!) * 1.4, 10),
            longitudeDelta: max((longitudes.max()! - longitudes.min()!) * 1.4, 10)
        )
        position = .region(MKCoordinateRegion(center: center, span: span))
    }
}
