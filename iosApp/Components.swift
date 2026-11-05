import SwiftUI
import Shared

struct StatusBannerView: View {
    @ObservedObject var model: FlightExplorerModel

    var body: some View {
        Group {
            if model.isLoading {
                banner(text: "Loading flights…", color: .blue.opacity(0.15))
            } else if model.isOfflineStale {
                HStack {
                    Text("Offline — showing cached flights")
                        .accessibilityLabel("Offline stale data banner")
                    Spacer()
                    Button("Retry") { model.refresh() }
                }
                .padding(12)
                .background(Color.orange.opacity(0.2))
                .clipShape(RoundedRectangle(cornerRadius: 10))
            } else if model.isError {
                VStack(alignment: .leading, spacing: 8) {
                    Text(model.errorMessage)
                        .accessibilityLabel("Error loading flights")
                    Button("Retry") { model.refresh() }
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.red.opacity(0.15))
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }
        }
    }

    private func banner(text: String, color: Color) -> some View {
        Text(text)
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(color)
            .clipShape(RoundedRectangle(cornerRadius: 10))
    }
}

struct FlightRowView: View {
    let flight: Flight
    let selected: Bool
    let saved: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(flight.origin) → \(flight.destination)")
                        .font(.headline)
                    Text("\(flight.originCity) to \(flight.destinationCity)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("\(flight.departureDate) · \(flight.cabin) · \(flight.program)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                VStack(alignment: .trailing) {
                    Text(flight.points.formatted())
                        .font(.headline)
                    Image(systemName: saved ? "bookmark.fill" : "bookmark")
                        .foregroundStyle(saved ? Color.orange : Color.secondary)
                        .accessibilityLabel(saved ? "Saved flight" : "Not saved")
                }
            }
            .padding(12)
            .background(selected ? Color.green.opacity(0.15) : Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(
            "Flight \(flight.origin) to \(flight.destination) on \(flight.departureDate), \(flight.points) points"
        )
    }
}

struct SelectionCardView: View {
    let flight: Flight
    let saved: Bool
    let onSave: () -> Void
    let onRemove: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("\(flight.origin) → \(flight.destination)")
                    .font(.title3.bold())
                Spacer()
                Button(action: onDismiss) {
                    Image(systemName: "xmark.circle.fill")
                }
                .accessibilityLabel("Clear selection")
            }
            Text("\(flight.originCity) → \(flight.destinationCity)")
            Text("Depart \(flight.departureDate)")
            Text("\(flight.cabin) · \(flight.program)")
            Text("\(flight.points.formatted()) points").bold()
            if saved {
                Button("Remove", action: onRemove)
                    .accessibilityLabel("Remove saved flight")
            } else {
                Button("Save", action: onSave)
                    .accessibilityLabel("Save flight")
            }
        }
        .padding()
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(radius: 4)
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Selected flight details")
    }
}

struct ActiveFilterChipsView: View {
    @ObservedObject var model: FlightExplorerModel

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("\(model.resultCount) flights")
                    .font(.headline)
                    .accessibilityLabel("Result count \(model.resultCount)")
                Spacer()
                if !model.originText.isEmpty || !model.destinationText.isEmpty || model.cabin != nil || model.maxPointsEnabled || !model.selectedPrograms.isEmpty {
                    Button("Reset filters") { model.resetFilters() }
                }
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack {
                    if !model.originText.trimmingCharacters(in: .whitespaces).isEmpty {
                        chip("From: \(model.originText)") {
                            model.onOriginChanged("")
                        }
                    }
                    if !model.destinationText.trimmingCharacters(in: .whitespaces).isEmpty {
                        chip("To: \(model.destinationText)") {
                            model.onDestinationChanged("")
                        }
                    }
                    if model.cabin != nil {
                        chip(model.cabin!) { model.onCabin(nil) }
                    }
                    if model.maxPointsEnabled {
                        chip("≤ \(Int(model.maxPoints).formatted()) pts") {
                            model.onMaxPointsChanged(model.maxPoints, enabled: false)
                        }
                    }
                    ForEach(Array(model.selectedPrograms).sorted(), id: \.self) { program in
                        chip(program) { model.toggleProgram(program) }
                    }
                }
            }
        }
    }

    private func chip(_ title: String, onRemove: @escaping () -> Void) -> some View {
        Button(action: onRemove) {
            Label(title, systemImage: "xmark")
                .font(.caption)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Color.green.opacity(0.15))
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}
