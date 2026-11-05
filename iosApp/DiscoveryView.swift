import Shared
import SwiftUI

struct DiscoveryView: View {
    @ObservedObject var model: FlightExplorerModel
    @State private var pane: DiscoverPane = .list

    private enum DiscoverPane: String, CaseIterable {
        case list = "List"
        case map = "Map"
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 10) {
                StatusBannerView(status: model.status) {
                    model.explorer.refresh()
                }

                ScrollView {
                    VStack(alignment: .leading, spacing: 10) {
                        TextField("Origin (code or city)", text: $model.originText)
                            .textFieldStyle(.roundedBorder)
                            .textInputAutocapitalization(.characters)
                            .accessibilityLabel("Origin search field")
                            .onChange(of: model.originText) { _, value in
                                model.explorer.setOriginQuery(raw: value)
                            }

                        TextField("Destination (code or city)", text: $model.destinationText)
                            .textFieldStyle(.roundedBorder)
                            .textInputAutocapitalization(.characters)
                            .accessibilityLabel("Destination search field")
                            .onChange(of: model.destinationText) { _, value in
                                model.explorer.setDestinationQuery(raw: value)
                            }

                        HStack {
                            TextField("From date", text: Binding(
                                get: { model.filters.dateFrom },
                                set: { model.explorer.setDateRange(from: $0, to: model.filters.dateTo) }
                            ))
                            .textFieldStyle(.roundedBorder)
                            .accessibilityLabel("Departure date from")

                            TextField("To date", text: Binding(
                                get: { model.filters.dateTo },
                                set: { model.explorer.setDateRange(from: model.filters.dateFrom, to: $0) }
                            ))
                            .textFieldStyle(.roundedBorder)
                            .accessibilityLabel("Departure date to")
                        }

                        Text("Cabin").font(.subheadline.weight(.semibold))
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack {
                                cabinChip(nil, label: "Any")
                                ForEach(cabinLabels, id: \.self) { cabin in
                                    cabinChip(cabin, label: cabin)
                                }
                            }
                        }

                        Text("Max points: \(Int(model.pointsSlider).formatted())")
                            .accessibilityLabel("Maximum points filter")
                        Slider(
                            value: $model.pointsSlider,
                            in: 9_000...133_000,
                            onEditingChanged: { editing in
                                if !editing {
                                    model.explorer.setMaxPointsValue(maxPoints: Int32(model.pointsSlider))
                                }
                            }
                        )
                        .accessibilityLabel("Maximum points slider")

                        Toggle(
                            model.filters.maxPoints == nil ? "Points filter off" : "Points filter on",
                            isOn: Binding(
                                get: { model.filters.maxPoints != nil },
                                set: { on in
                                    if on {
                                        model.explorer.setMaxPointsValue(maxPoints: Int32(model.pointsSlider))
                                    } else {
                                        model.explorer.clearMaxPoints()
                                    }
                                }
                            )
                        )

                        Text("Loyalty programs").font(.subheadline.weight(.semibold))
                        FlowPrograms(
                            selected: Set(model.filters.programs.map { "\($0)" }),
                            programs: programLabels,
                            onToggle: { model.explorer.toggleProgram(program: $0) }
                        )
                    }
                    .padding(.horizontal, 4)
                }
                .frame(maxHeight: 260)

                ActiveFilterChipsView(
                    filters: model.filters,
                    resultCount: model.filteredFlights.count,
                    onReset: {
                        model.originText = ""
                        model.destinationText = ""
                        model.pointsSlider = 133_000
                        model.explorer.resetFilters()
                    },
                    onClearOrigin: {
                        model.originText = ""
                        model.explorer.setOriginQuery(raw: "")
                    },
                    onClearDestination: {
                        model.destinationText = ""
                        model.explorer.setDestinationQuery(raw: "")
                    },
                    onClearCabin: { model.explorer.setCabin(cabin: nil) },
                    onClearMaxPoints: { model.explorer.clearMaxPoints() },
                    onClearDate: {
                        model.explorer.setDateRange(
                            from: FlightFilters.companion.DEFAULT_DATE_FROM,
                            to: FlightFilters.companion.DEFAULT_DATE_TO
                        )
                    },
                    onRemoveProgram: { model.explorer.toggleProgram(program: $0) }
                )

                Picker("Pane", selection: $pane) {
                    ForEach(DiscoverPane.allCases, id: \.self) { Text($0.rawValue).tag($0) }
                }
                .pickerStyle(.segmented)

                ZStack(alignment: .bottom) {
                    switch pane {
                    case .list:
                        if model.filteredFlights.isEmpty,
                           !(model.status is LoadStatusError),
                           !(model.status is LoadStatusLoading) {
                            ContentUnavailableView("No flights match these filters.", systemImage: "airplane")
                        } else {
                            List {
                                ForEach(model.filteredFlights, id: \.id) { flight in
                                    FlightRowView(
                                        flight: flight,
                                        selected: flight.id == model.selectedFlight?.id,
                                        saved: model.savedIds.contains(flight.id),
                                        onTap: { model.explorer.selectFlight(id: flight.id) }
                                    )
                                    .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 4, trailing: 0))
                                    .listRowSeparator(.hidden)
                                    .listRowBackground(Color.clear)
                                }
                            }
                            .listStyle(.plain)
                        }
                    case .map:
                        FlightMapView(
                            clusters: model.clusters,
                            selectedFlight: model.selectedFlight,
                            onClusterTap: { model.explorer.openCluster(cluster: $0) }
                        )
                    }

                    if let flight = model.selectedFlight {
                        SelectionCardView(
                            flight: flight,
                            saved: model.savedIds.contains(flight.id),
                            onSave: { _ = model.explorer.saveFlight(flight: flight) },
                            onRemove: { _ = model.explorer.removeSaved(id: flight.id) },
                            onDismiss: { model.explorer.selectFlight(id: nil) }
                        )
                        .padding(.horizontal, 8)
                        .padding(.bottom, 8)
                    }
                }
            }
            .padding(.horizontal, 16)
            .navigationTitle("Discover")
            .sheet(item: browsingBinding) { cluster in
                NavigationStack {
                    List {
                        Section {
                            Text("From \(cluster.lowestPoints.formatted()) points · \(cluster.destinationCity)")
                        }
                        ForEach(cluster.flights, id: \.id) { flight in
                            FlightRowView(
                                flight: flight,
                                selected: flight.id == model.selectedFlight?.id,
                                saved: model.savedIds.contains(flight.id),
                                onTap: {
                                    model.explorer.selectFlight(id: flight.id)
                                    model.explorer.dismissCluster()
                                }
                            )
                        }
                    }
                    .navigationTitle("\(cluster.destination) · \(cluster.flightCount) flights")
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Close") { model.explorer.dismissCluster() }
                        }
                    }
                }
            }
        }
    }

    private var cabinLabels: [String] {
        Cabin.companion.allLabels.map { "\($0)" }
    }

    private var programLabels: [String] {
        LoyaltyPrograms.shared.all.map { "\($0)" }
    }

    private var browsingBinding: Binding<DestinationCluster?> {
        Binding(
            get: { model.browsingCluster },
            set: { newValue in
                if newValue == nil { model.explorer.dismissCluster() }
            }
        )
    }

    private func cabinChip(_ cabin: String?, label: String) -> some View {
        let selected = model.filters.cabin == cabin
        return Button {
            model.explorer.setCabin(cabin: cabin)
        } label: {
            Text(label)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(selected ? Color.green.opacity(0.25) : Color(.tertiarySystemFill))
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Cabin filter \(label)")
    }
}

private struct FlowPrograms: View {
    let selected: Set<String>
    let programs: [String]
    let onToggle: (String) -> Void

    var body: some View {
        FlexibleChips(programs: programs, selected: selected, onToggle: onToggle)
    }
}

private struct FlexibleChips: View {
    let programs: [String]
    let selected: Set<String>
    let onToggle: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(programs, id: \.self) { program in
                Button {
                    onToggle(program)
                } label: {
                    Text(program)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(selected.contains(program) ? Color.green.opacity(0.25) : Color(.tertiarySystemFill))
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Loyalty program \(program)")
            }
        }
    }
}

extension DestinationCluster: @retroactive Identifiable {
    public var id: String { destination }
}
