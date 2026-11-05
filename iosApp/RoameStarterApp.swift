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
                        Label("Discover", systemImage: "airplane")
                    }
                    .accessibilityLabel("Discover tab")

                SavedView(model: model)
                    .tabItem {
                        Label("Saved", systemImage: "bookmark.fill")
                    }
                    .accessibilityLabel("Saved tab")
            }
        }
    }
}
