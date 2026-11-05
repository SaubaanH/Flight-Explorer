import Shared
import SwiftUI

struct SavedView: View {
    @ObservedObject var model: FlightExplorerModel

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottom) {
                if model.savedFlights.isEmpty {
                    ContentUnavailableView(
                        "No saved flights yet",
                        systemImage: "bookmark",
                        description: Text("Save one from Discover. Available offline.")
                    )
                } else {
                    List {
                        ForEach(model.savedFlights, id: \.id) { flight in
                            FlightRowView(
                                flight: flight,
                                selected: flight.id == model.selectedFlight?.id,
                                saved: true,
                                onTap: { model.explorer.selectFlight(id: flight.id) }
                            )
                            .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                            .listRowSeparator(.hidden)
                            .listRowBackground(Color.clear)
                        }
                    }
                    .listStyle(.plain)
                }

                if let flight = model.selectedFlight,
                   model.savedFlights.contains(where: { $0.id == flight.id }) {
                    SelectionCardView(
                        flight: flight,
                        saved: true,
                        onSave: { _ = model.explorer.saveFlight(flight: flight) },
                        onRemove: { _ = model.explorer.removeSaved(id: flight.id) },
                        onDismiss: { model.explorer.selectFlight(id: nil) }
                    )
                    .padding()
                }
            }
            .navigationTitle("Saved")
        }
    }
}
