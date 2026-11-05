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
                VStack(alignment: .leading, spacing: 12) {
                    RoameBrandHeader(
                        title: "Discover",
                        subtitle: "Find award flights with points"
                    )

                    TextField("Origin (code or city)", text: Binding(
                        get: { model.originText },
                        set: { model.onOriginChanged($0) }
                    ))
                    .padding(14)
                    .background(RoameTheme.surface)
                    .overlay(Capsule().stroke(RoameTheme.hairline, lineWidth: 1))
                    .clipShape(Capsule())
                    .accessibilityLabel("Origin search field")

                    TextField("Destination (code or city)", text: Binding(
                        get: { model.destinationText },
                        set: { model.onDestinationChanged($0) }
                    ))
                    .padding(14)
                    .background(RoameTheme.surface)
                    .overlay(Capsule().stroke(RoameTheme.hairline, lineWidth: 1))
                    .clipShape(Capsule())
                    .accessibilityLabel("Destination search field")

                    HStack(spacing: 8) {
                        TextField("From date", text: Binding(
                            get: { model.dateFrom },
                            set: { model.onDateFrom($0) }
                        ))
                        .padding(14)
                        .background(RoameTheme.surface)
                        .overlay(Capsule().stroke(RoameTheme.hairline, lineWidth: 1))
                        .clipShape(Capsule())
                        .accessibilityLabel("Departure date from")

                        TextField("To date", text: Binding(
                            get: { model.dateTo },
                            set: { model.onDateTo($0) }
                        ))
                        .padding(14)
                        .background(RoameTheme.surface)
                        .overlay(Capsule().stroke(RoameTheme.hairline, lineWidth: 1))
                        .clipShape(Capsule())
                        .accessibilityLabel("Departure date to")
                    }

                    Text("Cabin")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(RoameTheme.ink)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            RoameChip(label: "Any", selected: model.cabin == nil) {
                                model.onCabin(nil)
                            }
                            .accessibilityLabel("Cabin filter Any")
                            ForEach(["Economy", "Premium Economy", "Business"], id: \.self) { cabin in
                                RoameChip(label: cabin, selected: model.cabin == cabin) {
                                    model.onCabin(cabin)
                                }
                                .accessibilityLabel("Cabin filter \(cabin)")
                            }
                        }
                    }

                    Toggle(isOn: Binding(
                        get: { model.maxPointsEnabled },
                        set: { model.onMaxPointsChanged(model.maxPoints, enabled: $0) }
                    )) {
                        Text("Limit max points")
                            .foregroundStyle(RoameTheme.ink)
                    }
                    .tint(RoameTheme.pink)
                    .accessibilityLabel("Maximum points filter")

                    if model.maxPointsEnabled {
                        Text("Max points: \(Int(model.maxPoints).formatted())")
                            .foregroundStyle(RoameTheme.muted)
                        Slider(
                            value: Binding(
                                get: { model.maxPoints },
                                set: { model.onMaxPointsChanged($0, enabled: true) }
                            ),
                            in: 9_000...133_000,
                            step: 1_000
                        )
                        .tint(RoameTheme.pink)
                        .accessibilityLabel("Maximum points slider")
                    }

                    Text("Loyalty programs")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(RoameTheme.ink)
                    FlowLayout {
                        ForEach(LoyaltyPrograms.shared.all, id: \.self) { program in
                            let selected = model.selectedPrograms.contains(program)
                            RoameChip(label: program, selected: selected) {
                                model.toggleProgram(program)
                            }
                            .accessibilityLabel("Loyalty program \(program)")
                        }
                    }
                }
                .padding(16)
                .background(RoameTheme.surface)
                .overlay(
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .stroke(RoameTheme.hairline, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            }
            .frame(maxHeight: 300)

            ActiveFilterChipsView(model: model)

            paneToggle

            ZStack(alignment: .bottom) {
                switch pane {
                case .list:
                    if model.filteredFlights.isEmpty && !model.isLoading && !model.isError {
                        ContentUnavailableView("No flights match these filters.", systemImage: "airplane")
                            .foregroundStyle(RoameTheme.muted)
                    } else {
                        ScrollView {
                            LazyVStack(spacing: 12) {
                                ForEach(model.filteredFlights, id: \.id) { flight in
                                    FlightRowView(
                                        flight: flight,
                                        selected: model.selectedFlight?.id == flight.id,
                                        saved: model.isSaved(flight.id),
                                        onTap: { model.select(flight.id) }
                                    )
                                }
                            }
                            .padding(.bottom, 160)
                        }
                    }
                case .map:
                    FlightMapView(model: model)
                        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 20, style: .continuous)
                                .stroke(RoameTheme.hairline, lineWidth: 1)
                        )
                }

                if let flight = model.selectedFlight {
                    SelectionCardView(
                        flight: flight,
                        saved: model.isSaved(flight.id),
                        onSave: { model.save(flight) },
                        onRemove: { model.removeSaved(flight.id) },
                        onDismiss: { model.select(nil) }
                    )
                    .padding(.horizontal, 4)
                    .padding(.bottom, 8)
                }
            }
        }
        .padding(.horizontal)
        .background(RoameTheme.canvas.ignoresSafeArea())
        .alert("Save error", isPresented: Binding(
            get: { model.saveError != nil },
            set: { if !$0 { model.clearSaveError() } }
        )) {
            Button("OK", role: .cancel) { model.clearSaveError() }
        } message: {
            Text(model.saveError ?? "")
        }
    }

    private var paneToggle: some View {
        HStack(spacing: 4) {
            ForEach(Pane.allCases, id: \.self) { option in
                Button {
                    pane = option
                } label: {
                    Text(option.rawValue)
                        .font(.subheadline.weight(pane == option ? .bold : .medium))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .foregroundStyle(pane == option ? RoameTheme.pink : RoameTheme.muted)
                        .background(pane == option ? RoameTheme.pinkSoft : Color.clear)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .background(RoameTheme.surface)
        .overlay(Capsule().stroke(RoameTheme.hairline, lineWidth: 1))
        .clipShape(Capsule())
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
