import SwiftUI
import Shared

struct DiscoveryView: View {
    @ObservedObject var model: FlightExplorerModel
    @State private var pane: Pane = .list

    private enum Pane: String, CaseIterable {
        case list = "List"
        case map = "Map"
    }

    var body: some View {
        VStack(spacing: 12) {
            StatusBannerView(model: model)

            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    Text("Discover").font(.title2.bold())
                    TextField("Origin (code or city)", text: Binding(
                        get: { model.originText },
                        set: { model.onOriginChanged($0) }
                    ))
                    .textFieldStyle(.roundedBorder)
                    .accessibilityLabel("Origin search field")

                    TextField("Destination (code or city)", text: Binding(
                        get: { model.destinationText },
                        set: { model.onDestinationChanged($0) }
                    ))
                    .textFieldStyle(.roundedBorder)
                    .accessibilityLabel("Destination search field")

                    HStack {
                        TextField("From date", text: Binding(
                            get: { model.dateFrom },
                            set: { model.onDateFrom($0) }
                        ))
                        .textFieldStyle(.roundedBorder)
                        .accessibilityLabel("Departure date from")
                        TextField("To date", text: Binding(
                            get: { model.dateTo },
                            set: { model.onDateTo($0) }
                        ))
                        .textFieldStyle(.roundedBorder)
                        .accessibilityLabel("Departure date to")
                    }

                    Text("Cabin").font(.subheadline.bold())
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack {
                            cabinChip(nil, title: "Any")
                            ForEach(["Economy", "Premium Economy", "Business"], id: \.self) { cabin in
                                cabinChip(cabin, title: cabin)
                            }
                        }
                    }

                    Toggle("Limit max points", isOn: Binding(
                        get: { model.maxPointsEnabled },
                        set: { model.onMaxPointsChanged(model.maxPoints, enabled: $0) }
                    ))
                    .accessibilityLabel("Maximum points filter")
                    if model.maxPointsEnabled {
                        Text("Max points: \(Int(model.maxPoints).formatted())")
                        Slider(
                            value: Binding(
                                get: { model.maxPoints },
                                set: { model.onMaxPointsChanged($0, enabled: true) }
                            ),
                            in: 9_000...133_000,
                            step: 1_000
                        )
                        .accessibilityLabel("Maximum points slider")
                    }

                    Text("Loyalty programs").font(.subheadline.bold())
                    FlowLayout {
                        ForEach(LoyaltyPrograms.shared.all, id: \.self) { program in
                            let selected = model.selectedPrograms.contains(program)
                            Button {
                                model.toggleProgram(program)
                            } label: {
                                Text(program)
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 6)
                                    .background(selected ? Color.green.opacity(0.25) : Color(.secondarySystemBackground))
                                    .clipShape(Capsule())
                            }
                            .accessibilityLabel("Loyalty program \(program)")
                        }
                    }
                }
            }
            .frame(maxHeight: 260)

            ActiveFilterChipsView(model: model)

            Picker("Pane", selection: $pane) {
                ForEach(Pane.allCases, id: \.self) { Text($0.rawValue).tag($0) }
            }
            .pickerStyle(.segmented)

            ZStack(alignment: .bottom) {
                switch pane {
                case .list:
                    if model.filteredFlights.isEmpty && !model.isLoading && !model.isError {
                        ContentUnavailableView("No flights match these filters.", systemImage: "airplane")
                    } else {
                        ScrollView {
                            LazyVStack(spacing: 8) {
                                ForEach(model.filteredFlights, id: \.id) { flight in
                                    FlightRowView(
                                        flight: flight,
                                        selected: model.selectedFlight?.id == flight.id,
                                        saved: model.isSaved(flight.id),
                                        onTap: { model.select(flight.id) }
                                    )
                                }
                            }
                            .padding(.bottom, 140)
                        }
                    }
                case .map:
                    FlightMapView(model: model)
                }

                if let flight = model.selectedFlight {
                    SelectionCardView(
                        flight: flight,
                        saved: model.isSaved(flight.id),
                        onSave: { model.save(flight) },
                        onRemove: { model.removeSaved(flight.id) },
                        onDismiss: { model.select(nil) }
                    )
                    .padding()
                }
            }
        }
        .padding(.horizontal)
        .alert("Save error", isPresented: Binding(
            get: { model.saveError != nil },
            set: { if !$0 { model.clearSaveError() } }
        )) {
            Button("OK", role: .cancel) { model.clearSaveError() }
        } message: {
            Text(model.saveError ?? "")
        }
    }

    private func cabinChip(_ value: String?, title: String) -> some View {
        let selected = model.cabin == value
        return Button {
            model.onCabin(value)
        } label: {
            Text(title)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(selected ? Color.green.opacity(0.25) : Color(.secondarySystemBackground))
                .clipShape(Capsule())
        }
        .accessibilityLabel("Cabin filter \(title)")
    }
}

/// Simple wrapping layout for program chips.
struct FlowLayout: Layout {
    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = layout(in: proposal.width ?? 0, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = layout(in: bounds.width, subviews: subviews)
        for (index, point) in result.origins.enumerated() {
            subviews[index].place(
                at: CGPoint(x: bounds.minX + point.x, y: bounds.minY + point.y),
                proposal: .unspecified
            )
        }
    }

    private func layout(in width: CGFloat, subviews: Subviews) -> (size: CGSize, origins: [CGPoint]) {
        var origins: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        let spacing: CGFloat = 8
        for sub in subviews {
            let size = sub.sizeThatFits(.unspecified)
            if x + size.width > width, x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            origins.append(CGPoint(x: x, y: y))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
        }
        return (CGSize(width: width, height: y + rowHeight), origins)
    }
}
