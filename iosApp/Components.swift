import Shared
import SwiftUI

struct StatusBannerView: View {
    let status: LoadStatus
    let onRetry: () -> Void

    var body: some View {
        if status is LoadStatusLoading {
            banner("Loading flights…", color: .secondary)
        } else if status is LoadStatusOfflineStale {
            HStack {
                Text("Offline — showing cached flights")
                    .accessibilityLabel("Offline stale data banner")
                Spacer()
                Button("Retry", action: onRetry)
            }
            .padding(12)
            .background(Color.orange.opacity(0.2))
            .clipShape(RoundedRectangle(cornerRadius: 10))
        } else if let error = status as? LoadStatusError {
            VStack(alignment: .leading, spacing: 8) {
                Text(error.message)
                Button("Retry", action: onRetry)
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.red.opacity(0.15))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .accessibilityLabel("Error loading flights")
        }
    }

    private func banner(_ text: String, color: Color) -> some View {
        Text(text)
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(color.opacity(0.15))
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
            HStack(alignment: .top) {
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
                VStack(alignment: .trailing, spacing: 4) {
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
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("\(flight.origin) → \(flight.destination)")
                    .font(.title3.bold())
                Spacer()
                Button(action: onDismiss) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
                .accessibilityLabel("Clear selection")
            }
            Text("\(flight.originCity) → \(flight.destinationCity)")
            Text("Depart \(flight.departureDate)")
            Text("\(flight.cabin) · \(flight.program)")
            Text("\(flight.points.formatted()) points")
                .fontWeight(.semibold)
            if saved {
                Button("Remove", action: onRemove)
                    .accessibilityLabel("Remove saved flight")
            } else {
                Button("Save", action: onSave)
                    .accessibilityLabel("Save flight")
            }
        }
        .padding(16)
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .shadow(radius: 4)
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Selected flight details")
    }
}

struct ActiveFilterChipsView: View {
    let filters: FlightFilters
    let resultCount: Int
    let onReset: () -> Void
    let onClearOrigin: () -> Void
    let onClearDestination: () -> Void
    let onClearCabin: () -> Void
    let onClearMaxPoints: () -> Void
    let onClearDate: () -> Void
    let onRemoveProgram: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("\(resultCount) flights")
                    .font(.subheadline.weight(.semibold))
                    .accessibilityLabel("Result count \(resultCount)")
                Spacer()
                if !filters.isDefault() {
                    Button("Reset filters", action: onReset)
                }
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    if !filters.originQuery.trimmingCharacters(in: .whitespaces).isEmpty {
                        chip("From: \(filters.originQuery)", onClearOrigin)
                    }
                    if !filters.destinationQuery.trimmingCharacters(in: .whitespaces).isEmpty {
                        chip("To: \(filters.destinationQuery)", onClearDestination)
                    }
                    if filters.dateFrom != FlightFilters.companion.DEFAULT_DATE_FROM
                        || filters.dateTo != FlightFilters.companion.DEFAULT_DATE_TO {
                        chip("\(filters.dateFrom) → \(filters.dateTo)", onClearDate)
                    }
                    if let cabin = filters.cabin {
                        chip(cabin, onClearCabin)
                    }
                    if let maxBoxed = filters.maxPoints {
                        chip("≤ \(maxBoxed.intValue.formatted()) pts", onClearMaxPoints)
                    }
                    ForEach(filters.programs.map { "\($0)" }, id: \.self) { program in
                        chip(program) { onRemoveProgram(program) }
                    }
                }
            }
        }
    }

    private func chip(_ title: String, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 4) {
                Text(title)
                Image(systemName: "xmark")
                    .font(.caption2)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(Color(.tertiarySystemFill))
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}
