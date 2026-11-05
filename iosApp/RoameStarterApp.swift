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
        }
    }
}
