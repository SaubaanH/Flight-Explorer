import SwiftUI
import Shared

struct DiscoveryView: View {
    @ObservedObject var model: FlightExplorerModel
    @Namespace private var searchNS
    @State private var isSearching = false
    @State private var pane: Pane = .list
    /// Matches `FlightExplorer.LIST_PAGE_SIZE` — keep in sync.
    @State private var listLimit = 24

    private enum Pane: String, CaseIterable {
        case list = "List"
        case map = "Map"
    }

    private let pageSize = 24

    private var searchSpring: Animation {
        .spring(duration: 0.38, bounce: 0.12)
    }

    var body: some View {
        ZStack {
            discoverContent
                .padding(.horizontal, isSearching ? 0 : 16)
                .opacity(isSearching ? 0 : 1)

            if isSearching {
                SearchScreen(
                    model: model,
                    namespace: searchNS,
                    onClose: closeSearch
                )
                .transition(.opacity)
                .zIndex(1)
            }
        }
        .background(RoameTheme.canvas.ignoresSafeArea())
        .toolbar(isSearching ? .hidden : .automatic, for: .tabBar)
        .animation(searchSpring, value: isSearching)
        .alert("Save error", isPresented: Binding(
            get: { model.saveError != nil },
            set: { if !$0 { model.clearSaveError() } }
        )) {
            Button("OK", role: .cancel) { model.clearSaveError() }
        } message: {
            Text(model.saveError ?? "")
        }
    }

    private var discoverContent: some View {
        VStack(spacing: 12) {
            StatusBannerView(model: model)

            HStack {
                RoameBrandHeader(
                    title: "Discover",
                    subtitle: "Find award flights with points"
                )
                Spacer(minLength: 0)
            }

            if !isSearching {
                SearchPill(
                    summary: SearchSummary.text(model: model),
                    detail: SearchSummary.detail(model: model),
                    namespace: searchNS,
                    onTap: openSearch
                )
            } else {
                // Hold layout space so matchedGeometry has a stable source frame.
                Color.clear.frame(height: 64)
            }

            ActiveFilterChipsView(model: model)

            paneToggle

            ZStack(alignment: .bottom) {
                switch pane {
                case .list:
                    if model.flightCards.isEmpty && !model.isLoading && !model.isError {
                        ContentUnavailableView("No flights match these filters.", systemImage: "airplane")
                            .foregroundStyle(RoameTheme.muted)
                    } else {
                        WindowedFlightList(
                            cards: model.flightCards,
                            selectedId: model.selectedFlight?.id,
                            savedIds: model.savedIds,
                            listLimit: $listLimit,
                            pageSize: pageSize,
                            onSelect: { model.select($0) }
                        )
                        .onChange(of: model.resultCount) { _, _ in
                            listLimit = min(pageSize, model.flightCards.count)
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

                if let flight = model.selectedFlight, !isSearching {
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

    private func openSearch() {
        withAnimation(searchSpring) { isSearching = true }
    }

    private func closeSearch() {
        withAnimation(searchSpring) { isSearching = false }
    }
}

private struct WindowedFlightList: View {
    let cards: [FlightCard]
    let selectedId: String?
    let savedIds: Set<String>
    @Binding var listLimit: Int
    let pageSize: Int
    let onSelect: (String) -> Void

    var body: some View {
        let shown = Array(cards.prefix(listLimit))
        List {
            ForEach(shown) { card in
                FlightRowView(
                    card: card,
                    selected: selectedId == card.id,
                    saved: savedIds.contains(card.id),
                    onTap: { onSelect(card.id) }
                )
                .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 4, trailing: 0))
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)
            }
            if listLimit < cards.count {
                Text("Showing \(listLimit) of \(cards.count)…")
                    .font(.caption)
                    .foregroundStyle(RoameTheme.muted)
                    .frame(maxWidth: .infinity)
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
                    .onAppear {
                        listLimit = min(listLimit + pageSize, cards.count)
                    }
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .environment(\.defaultMinListRowHeight, 1)
        .safeAreaInset(edge: .bottom) { Color.clear.frame(height: 140) }
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
