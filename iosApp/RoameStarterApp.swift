import SwiftUI
import Shared

@main
struct RoameStarterApp: App {
    @StateObject private var model = FlightExplorerModel()

    var body: some Scene {
        WindowGroup {
            ZStack {
                TabView {
                    DiscoveryView(model: model)
                        .tabItem {
                            Label("Discover", systemImage: "safari")
                        }
                        .accessibilityLabel("Discover tab")

                    SavedView(model: model)
                        .tabItem {
                            Label("Saved", systemImage: "heart")
                        }
                        .accessibilityLabel("Saved tab")
                }
                .tint(RoameTheme.pink)
                .background(RoameTheme.canvas.ignoresSafeArea())

                // Above the tab bar — local overlays inside each tab sat under it.
                if let flight = model.selectedFlight {
                    FlightDetailOverlay(
                        flight: flight,
                        saved: model.isSaved(flight.id),
                        onSave: { model.save(flight) },
                        onRemove: {
                            model.removeSaved(flight.id)
                            withAnimation(FlightDetailPresentation.dismissAnimation) {
                                model.select(nil)
                            }
                        },
                        onDismiss: {
                            withAnimation(FlightDetailPresentation.dismissAnimation) {
                                model.select(nil)
                            }
                        }
                    )
                    .transition(.opacity)
                    .zIndex(100)
                }
            }
            .animation(FlightDetailPresentation.openAnimation, value: model.selectedFlight?.id)
            .task {
                SharedRouteMapLibre.shared.prewarm()
            }
            .alert("Save error", isPresented: Binding(
                get: { model.saveError != nil },
                set: { if !$0 { model.clearSaveError() } }
            )) {
                Button("OK", role: .cancel) { model.clearSaveError() }
            } message: {
                Text(model.saveError ?? "")
            }
        }
    }
}
