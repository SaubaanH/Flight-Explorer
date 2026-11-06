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

                // Separate inserts so scrim can fade while the panel slides up.
                if model.selectedFlight != nil {
                    Color.black.opacity(FlightDetailPresentation.scrimOpacity)
                        .ignoresSafeArea()
                        .onTapGesture {
                            withAnimation(FlightDetailPresentation.dismissAnimation) {
                                model.select(nil)
                            }
                        }
                        .accessibilityLabel("Dismiss flight details")
                        .transition(.opacity)
                        .zIndex(99)
                }

                if let flight = model.selectedFlight {
                    VStack(spacing: 0) {
                        Spacer(minLength: 0)
                        FlightDetailSheet(
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
                        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
                        .shadow(color: .black.opacity(0.12), radius: 24, y: -4)
                    }
                    .ignoresSafeArea(edges: .bottom)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
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
