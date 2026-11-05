import SwiftUI
import Shared

struct SavedView: View {
    @ObservedObject var model: FlightExplorerModel

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
        .toolbar(model.selectedFlight != nil ? .hidden : .automatic, for: .tabBar)
    }

    private func openDetail(_ id: String) {
        guard let flight = model.flight(for: id) else { return }
        SharedRouteMapLibre.shared.prepare(flight)
        withAnimation(FlightDetailPresentation.openAnimation) {
            model.select(id)
        }
    }
}
