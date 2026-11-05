import Foundation
import Shared
import Combine

/// Observable bridge over the shared `FlightExplorer` facade.
/// Polls StateFlow `.value` so SwiftUI stays in lockstep without SKIE.
@MainActor
final class FlightExplorerModel: ObservableObject {
    let explorer: FlightExplorer

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

    private var timer: Timer?

    init(baseUrl: String = ApiBaseUrls.shared.IOS_SIMULATOR) {
        explorer = FlightExplorerFactory.shared.create(
            baseUrl: baseUrl,
            driverFactory: DatabaseDriverFactory()
        )
        timer = Timer.scheduledTimer(withTimeInterval: 0.15, repeats: true) { [weak self] _ in
            Task { @MainActor in
                self?.pull()
            }
        }
        pull()
    }

    deinit {
        timer?.invalidate()
        explorer.close()
    }

    func pull() {
        let filters = explorer.filters.value
        dateFrom = filters.dateFrom
        dateTo = filters.dateTo
        cabin = filters.cabin
        if let max = filters.maxPoints?.intValue {
            maxPointsEnabled = true
            maxPoints = Double(max)
        } else {
            maxPointsEnabled = false
        }
        var programs = Set<String>()
        for item in filters.programs {
            if let program = item as? String {
                programs.insert(program)
            }
        }
        selectedPrograms = programs

        filteredFlights = (explorer.filteredFlights.value as? [Any] ?? []).compactMap { $0 as? Flight }
        clusters = (explorer.clusters.value as? [Any] ?? []).compactMap { $0 as? DestinationCluster }
        selectedFlight = explorer.selectedFlight.value
        savedFlights = (explorer.savedFlights.value as? [Any] ?? []).compactMap { $0 as? Flight }
        var ids = Set<String>()
        for item in explorer.savedIds.value {
            if let id = item as? String { ids.insert(id) }
        }
        savedIds = ids
        browsingCluster = explorer.browsingCluster.value
        resultCount = filteredFlights.count
        saveError = explorer.saveError.value

        let status = explorer.status.value
        let kind = status.kind
        isLoading = kind == "loading"
        isOfflineStale = kind == "offline_stale"
        isError = kind == "error"
        errorMessage = status.errorMessageOrNull ?? ""
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
