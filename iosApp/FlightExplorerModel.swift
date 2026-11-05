import Foundation
import Combine
@preconcurrency import Shared

/// Observable bridge over shared `FlightExplorer` via `FlightExplorerIos` snapshots.
@MainActor
final class FlightExplorerModel: ObservableObject {
    // nonisolated(unsafe): KMP types are not Sendable; deinit must still close the host.
    nonisolated(unsafe) private let host: FlightExplorerIos
    var explorer: FlightExplorer { host.explorer }

    @Published var originText: String = ""
    @Published var destinationText: String = ""
    @Published var dateFrom: String = "2027-02-01"
    @Published var dateTo: String = "2027-04-01"
    @Published var cabin: String? = nil
    @Published var maxPointsEnabled: Bool = false
    @Published var maxPoints: Double = 133_000
    @Published var selectedPrograms: Set<String> = []

    @Published var filteredFlights: [Flight] = []
    @Published var clusters: [DestinationCluster] = []
    @Published var selectedFlight: Flight? = nil
    @Published var savedFlights: [Flight] = []
    @Published var savedIds: Set<String> = []
    @Published var browsingCluster: DestinationCluster? = nil

    @Published var isLoading: Bool = true
    @Published var isOfflineStale: Bool = false
    @Published var isError: Bool = false
    @Published var errorMessage: String = ""
    @Published var resultCount: Int = 0
    @Published var saveError: String? = nil

    init(baseUrl: String = ApiBaseUrls.shared.IOS_SIMULATOR) {
        host = FlightExplorerIos(baseUrl: baseUrl)
        host.start { [weak self] snapshot in
            Task { @MainActor in
                self?.apply(snapshot)
            }
        }
    }

    deinit {
        host.close()
    }

    private func apply(_ snap: FlightUiSnapshot) {
        // Status first so the banner clears even if bridging a large flight list is slow.
        let kind = snap.status.kind
        isLoading = kind == "loading"
        isOfflineStale = kind == "offline_stale"
        isError = kind == "error"
        errorMessage = snap.status.errorMessageOrNull ?? ""
        saveError = snap.saveError

        let filters = snap.filters
        dateFrom = filters.dateFrom
        dateTo = filters.dateTo
        cabin = filters.cabin
        if let max = filters.maxPoints?.intValue {
            maxPointsEnabled = true
            maxPoints = Double(max)
        } else {
            maxPointsEnabled = false
        }
        selectedPrograms = Set(filters.programs)
        filteredFlights = snap.filteredFlights
        clusters = snap.clusters
        selectedFlight = snap.selectedFlight
        savedFlights = snap.savedFlights
        savedIds = Set(snap.savedIds)
        browsingCluster = snap.browsingCluster
        resultCount = filteredFlights.count
    }

    func refresh() { explorer.refresh() }

    func onOriginChanged(_ text: String) {
        originText = text
        explorer.setOriginQuery(raw: text)
    }

    func onDestinationChanged(_ text: String) {
        destinationText = text
        explorer.setDestinationQuery(raw: text)
    }

    func onDateFrom(_ text: String) {
        dateFrom = text
        explorer.setDateRange(from: text, to: dateTo)
    }

    func onDateTo(_ text: String) {
        dateTo = text
        explorer.setDateRange(from: dateFrom, to: text)
    }

    func onCabin(_ value: String?) {
        cabin = value
        explorer.setCabin(cabin: value)
    }

    func onMaxPointsChanged(_ value: Double, enabled: Bool) {
        maxPoints = value
        maxPointsEnabled = enabled
        if enabled {
            explorer.setMaxPoints(maxPoints: KotlinInt(int: Int32(value)))
        } else {
            explorer.setMaxPoints(maxPoints: nil)
        }
    }

    func toggleProgram(_ program: String) {
        explorer.toggleProgram(program: program)
    }

    func resetFilters() {
        originText = ""
        destinationText = ""
        maxPointsEnabled = false
        maxPoints = 133_000
        explorer.resetFilters()
    }

    func select(_ id: String?) { explorer.selectFlight(id: id) }
    func openCluster(_ cluster: DestinationCluster) { explorer.openCluster(cluster: cluster) }
    func dismissCluster() { explorer.dismissCluster() }
    func save(_ flight: Flight) { _ = explorer.saveFlight(flight: flight) }
    func removeSaved(_ id: String) { _ = explorer.removeSaved(id: id) }
    func clearSaveError() { explorer.clearSaveError() }

    func isSaved(_ id: String) -> Bool { savedIds.contains(id) }
}
