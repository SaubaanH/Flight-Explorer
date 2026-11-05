import SwiftUI
import Shared

@main
struct RoameStarterApp: App {
    @StateObject private var model = FlightExplorerModel()

    var body: some Scene {
        WindowGroup {
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
            .task {
                // Load MapLibre JS/style while the user browses — sheet open only fits the route.
                SharedRouteMapLibre.shared.prewarm()
            }
            // App-root so Discover and Saved both surface persistence failures.
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
