import Foundation
import Shared
import SwiftUI

@MainActor
final class FlightExplorerModel: ObservableObject {
    let host: FlightExplorerIos
    var explorer: FlightExplorer { host.explorer }

    @Published var filters: FlightFilters = FlightFilters.companion.defaults()
    @Published var filteredFlights: [Flight] = []
    @Published var clusters: [DestinationCluster] = []
    @Published var selectedFlight: Flight?
    @Published var savedFlights: [Flight] = []
    @Published var savedIds: Set<String> = []
    @Published var status: LoadStatus = LoadStatusLoading()
    @Published var browsingCluster: DestinationCluster?
    @Published var saveError: String?
    @Published var originText: String = ""
    @Published var destinationText: String = ""
    @Published var pointsSlider: Double = 133_000

    init(host: FlightExplorerIos = FlightExplorerIos()) {
        self.host = host
        host.start { [weak self] snapshot in
            Task { @MainActor in
                self?.apply(snapshot)
            }
        }
    }

    private func apply(_ snapshot: FlightUiSnapshot) {
        filters = snapshot.filters
        filteredFlights = snapshot.filteredFlights
        clusters = snapshot.clusters
        selectedFlight = snapshot.selectedFlight
        savedFlights = snapshot.savedFlights
        savedIds = Set(snapshot.savedIds.map { $0 as String })
        status = snapshot.status
        browsingCluster = snapshot.browsingCluster
        saveError = snapshot.saveError
    }

    func close() {
        host.close()
    }
}
