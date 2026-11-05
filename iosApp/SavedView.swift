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
                                onTap: { model.select(card.id) }
                            )
                        }
                    }
                    .padding(.bottom, 24)
                }
            }
        }
        .padding(.horizontal)
        .background(RoameTheme.canvas.ignoresSafeArea())
        .sheet(item: selectedSavedItem) { item in
            FlightDetailSheet(
                flight: item.flight,
                saved: true,
                onSave: { model.save(item.flight) },
                onRemove: { model.removeSaved(item.flight.id) },
                onDismiss: { model.select(nil) }
            )
        }
    }

    private var selectedSavedItem: Binding<SelectedFlightItem?> {
        Binding(
            get: {
                guard let flight = model.selectedFlight, model.isSaved(flight.id) else { return nil }
                return SelectedFlightItem(flight: flight)
            },
            set: { if $0 == nil { model.select(nil) } }
        )
    }
}
