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

            ZStack(alignment: .bottom) {
                if model.savedFlights.isEmpty {
                    ContentUnavailableView(
                        "No saved flights yet",
                        systemImage: "bookmark",
                        description: Text("Save one from Discover.")
                    )
                    .foregroundStyle(RoameTheme.muted)
                } else {
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(model.savedFlights, id: \.id) { flight in
                                FlightRowView(
                                    flight: flight,
                                    selected: model.selectedFlight?.id == flight.id,
                                    saved: true,
                                    onTap: { model.select(flight.id) }
                                )
                            }
                        }
                        .padding(.bottom, 160)
                    }
                }

                if let flight = model.selectedFlight, model.isSaved(flight.id) {
                    SelectionCardView(
                        flight: flight,
                        saved: true,
                        onSave: { model.save(flight) },
                        onRemove: { model.removeSaved(flight.id) },
                        onDismiss: { model.select(nil) }
                    )
                    .padding(.bottom, 8)
                }
            }
        }
        .padding(.horizontal)
        .background(RoameTheme.canvas.ignoresSafeArea())
    }
}
