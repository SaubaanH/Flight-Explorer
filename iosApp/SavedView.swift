import SwiftUI
import Shared

struct SavedView: View {
    @ObservedObject var model: FlightExplorerModel
    @State private var sheetItem: SelectedFlightItem? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            RoameBrandHeader(
                title: "Saved flights",
                subtitle: "Independent of discovery filters. Available offline."
            )

            if model.savedCards.isEmpty {
                ContentUnavailableView(
                    "No saved flights yet",
                    systemImage: "bookmark",
                    description: Text("Save one from Discover.")
                )
                .foregroundStyle(RoameTheme.muted)
            } else {
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(model.savedCards) { card in
                            FlightRowView(
                                card: card,
                                selected: model.selectedFlight?.id == card.id,
                                saved: true,
                                onTap: { openDetail(card.id) }
                            )
                        }
                    }
                    .padding(.bottom, 24)
                }
            }
        }
        .padding(.horizontal)
        .background(RoameTheme.canvas.ignoresSafeArea())
        .flightDetailOverlay(
            item: $sheetItem,
            saved: { _ in true },
            onSave: { model.save($0) },
            onRemove: { flight in
                model.removeSaved(flight.id)
                model.select(nil)
            },
            onDismiss: { model.select(nil) }
        )
        .onChange(of: model.selectedFlight?.id) { _, newId in
            if newId == nil { sheetItem = nil }
        }
    }

    private func openDetail(_ id: String) {
        model.select(id)
        guard let flight = model.flight(for: id) else { return }
        SharedRouteMapLibre.shared.prepare(flight)
        withAnimation(FlightDetailPresentation.openAnimation) {
            sheetItem = SelectedFlightItem(flight: flight)
        }
    }
}
