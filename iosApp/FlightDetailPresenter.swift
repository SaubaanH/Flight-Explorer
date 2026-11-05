import SwiftUI
import Shared

/// Sheet timing — long enough for MapLibre fit on tap, snappier than before.
enum FlightDetailPresentation {
    static let openAnimation: Animation = .spring(duration: 0.42, bounce: 0.04)
    static let dismissAnimation: Animation = .spring(duration: 0.32, bounce: 0.0)
}

/// Custom bottom sheet drawn above the TabView so Discover/Saved tabs stay under it.
struct FlightDetailOverlay: View {
    let flight: Flight
    let saved: Bool
    let onSave: () -> Void
    let onRemove: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        ZStack(alignment: .bottom) {
            Color.black.opacity(0.4)
                .ignoresSafeArea()
                .onTapGesture(perform: onDismiss)
                .accessibilityLabel("Dismiss flight details")

            FlightDetailSheet(
                flight: flight,
                saved: saved,
                onSave: onSave,
                onRemove: onRemove,
                onDismiss: onDismiss
            )
            .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
            .shadow(color: .black.opacity(0.12), radius: 24, y: -4)
            .transition(.move(edge: .bottom).combined(with: .opacity))
        }
        .ignoresSafeArea()
    }
}
