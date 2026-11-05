import Shared
import SwiftUI

@main
struct RoameStarterApp: App {
    @StateObject private var model = FlightExplorerModel()

    var body: some Scene {
        WindowGroup {
            RootTabView(model: model)
                .onAppear {
                    // Ensure JAVA-less iOS still documents the local API expectation.
                }
                .alert("Save error", isPresented: Binding(
                    get: { model.saveError != nil },
                    set: { if !$0 { model.explorer.clearSaveError() } }
                )) {
                    Button("OK", role: .cancel) { model.explorer.clearSaveError() }
                } message: {
                    Text(model.saveError ?? "")
                }
        }
    }
}

struct RootTabView: View {
    @ObservedObject var model: FlightExplorerModel

    var body: some View {
        TabView {
            DiscoveryView(model: model)
                .tabItem {
                    Label("Discover", systemImage: "globe.americas.fill")
                }
                .accessibilityLabel("Discover tab")

            SavedView(model: model)
                .tabItem {
                    Label("Saved", systemImage: "bookmark.fill")
                }
                .accessibilityLabel("Saved tab")
        }
        .tint(Color(red: 0.04, green: 0.43, blue: 0.31))
    }
}
