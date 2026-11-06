import SwiftUI
import Shared

enum FlightDetailPresentation {
    /// Material FastOutSlowIn @ 420ms — same as Android `tween(SheetOpenMillis)`.
    static let openAnimation: Animation = .timingCurve(0.4, 0.0, 0.2, 1.0, duration: 0.42)
    static let dismissAnimation: Animation = .timingCurve(0.4, 0.0, 1.0, 1.0, duration: 0.30)
    /// Soft scrim — matched with Android detail sheet.
    static let scrimOpacity: Double = 0.28
}

/// Custom bottom sheet above the TabView. Scrim fades; panel slides up (like Android).
struct FlightDetailOverlay: View {
    let flight: Flight
    let saved: Bool
    let onSave: () -> Void
    let onRemove: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        ZStack(alignment: .bottom) {
            Color.black.opacity(FlightDetailPresentation.scrimOpacity)
                .ignoresSafeArea()
                .onTapGesture(perform: onDismiss)
                .accessibilityLabel("Dismiss flight details")
                .transition(.opacity)

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
