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

    /// Discover list rows (Swift value types — cheap to diff/scroll).
    @Published var flightCards: [FlightCard] = []
    @Published var savedCards: [FlightCard] = []
    @Published var clusters: [DestinationCluster] = []
    @Published var selectedFlight: Flight? = nil
    /// Saved-tab MapLibre detail sheet only — Discover uses in-list expand + map route.
    @Published var detailSheetPresented = false
    @Published var savedIds: Set<String> = []
    @Published var browsingCluster: DestinationCluster? = nil

    @Published var isLoading: Bool = true
    @Published var isOfflineStale: Bool = false
    @Published var isError: Bool = false
    @Published var isEmptyResponse: Bool = false
    @Published var errorMessage: String = ""
    @Published var resultCount: Int = 0
    @Published var saveError: String? = nil

    var datesAreDefault: Bool {
        dateFrom == FlightFilters.companion.DEFAULT_DATE_FROM &&
            dateTo == FlightFilters.companion.DEFAULT_DATE_TO
    }

    var hasActiveFilters: Bool {
        !originText.trimmingCharacters(in: .whitespaces).isEmpty ||
            !destinationText.trimmingCharacters(in: .whitespaces).isEmpty ||
            !datesAreDefault ||
            cabin != nil ||
            maxPointsEnabled ||
            !selectedPrograms.isEmpty
    }

    /// Full Kotlin flights for actions (save/select) — not observed by the list.
    private var flightsById: [String: Flight] = [:]
    private var savedById: [String: Flight] = [:]
    private var filteredGeneration: Int64 = -1

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
        isEmptyResponse = kind == "empty"
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

        // Only re-bridge the filtered list when generation bumps (not on selection/save).
        let generation = snap.filteredGeneration
        if generation != filteredGeneration {
            filteredGeneration = generation
            let flights = snap.filteredFlights
            flightsById = Dictionary(uniqueKeysWithValues: flights.map { ($0.id, $0) })
            flightCards = flights.map(FlightCard.init)
            resultCount = flightCards.count
        }

        let newSavedIds = Set(snap.savedIds)
        if newSavedIds != savedIds {
            let saved = snap.savedFlights
            savedById = Dictionary(uniqueKeysWithValues: saved.map { ($0.id, $0) })
            savedCards = saved.map(FlightCard.init)
            savedIds = newSavedIds
        }
        clusters = snap.clusters
        selectedFlight = snap.selectedFlight
        if snap.selectedFlight == nil {
            detailSheetPresented = false
        }
        browsingCluster = snap.browsingCluster
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
        dateFrom = FlightFilters.companion.DEFAULT_DATE_FROM
        dateTo = FlightFilters.companion.DEFAULT_DATE_TO
        maxPointsEnabled = false
        maxPoints = 133_000
        explorer.resetFilters()
    }

    func clearDateRange() {
        dateFrom = FlightFilters.companion.DEFAULT_DATE_FROM
        dateTo = FlightFilters.companion.DEFAULT_DATE_TO
        explorer.setDateRange(from: dateFrom, to: dateTo)
    }

    func select(_ id: String?) {
        explorer.selectFlight(id: id)
        if id == nil { detailSheetPresented = false }
    }

    func presentDetailSheet(for id: String) {
        guard flight(for: id) != nil else { return }
        detailSheetPresented = true
        explorer.selectFlight(id: id)
    }

    func dismissDetailSheet() {
        detailSheetPresented = false
        explorer.selectFlight(id: nil)
    }

    func openCluster(_ cluster: DestinationCluster) { explorer.openCluster(cluster: cluster) }
    func dismissCluster() { explorer.dismissCluster() }

    func save(_ flight: Flight) { explorer.saveFlight(flight: flight) }
    func save(cardId: String) {
        if let flight = flightsById[cardId] ?? savedById[cardId] ?? selectedFlight {
            explorer.saveFlight(flight: flight)
        }
    }

    func removeSaved(_ id: String) { explorer.removeSaved(id: id) }
    func clearSaveError() { explorer.clearSaveError() }

    func isSaved(_ id: String) -> Bool { savedIds.contains(id) }

    func flight(for id: String) -> Flight? {
        flightsById[id] ?? savedById[id] ?? selectedFlight
    }
}
