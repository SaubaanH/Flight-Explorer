import SwiftUI
import Shared

/// Slow spring sheet so MapLibre can finish fitBounds between tap and settle.
enum FlightDetailPresentation {
    static let openAnimation: Animation = .spring(duration: 0.58, bounce: 0.04)
    static let dismissAnimation: Animation = .spring(duration: 0.42, bounce: 0.0)
}

/// Custom bottom sheet (not system `.sheet`) so we own open duration.
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
            .padding(.horizontal, 0)
            .transition(.move(edge: .bottom).combined(with: .opacity))
        }
        .ignoresSafeArea(edges: .bottom)
    }
}

extension View {
    /// Prepares the route map on tap, then opens a slower custom sheet so the map
    /// is fitted by the time the sheet settles.
    func flightDetailOverlay(
        item: Binding<SelectedFlightItem?>,
        saved: @escaping (Flight) -> Bool,
        onSave: @escaping (Flight) -> Void,
        onRemove: @escaping (Flight) -> Void,
        onDismiss: @escaping () -> Void
    ) -> some View {
        ZStack {
            self
            if let current = item.wrappedValue {
                FlightDetailOverlay(
                    flight: current.flight,
                    saved: saved(current.flight),
                    onSave: { onSave(current.flight) },
                    onRemove: { onRemove(current.flight) },
                    onDismiss: {
                        withAnimation(FlightDetailPresentation.dismissAnimation) {
                            item.wrappedValue = nil
                        }
                        onDismiss()
                    }
                )
                .zIndex(20)
                .transition(.opacity)
            }
        }
        .animation(FlightDetailPresentation.openAnimation, value: item.wrappedValue?.id)
    }
}
