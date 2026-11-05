import SwiftUI
import Shared

struct StatusBannerView: View {
    @ObservedObject var model: FlightExplorerModel

    var body: some View {
        Group {
            if model.isLoading {
                banner(text: "Loading flights…", color: RoameTheme.pinkSoft)
            } else if model.isOfflineStale {
                HStack {
                    Text("Offline — showing cached flights")
                        .accessibilityLabel("Offline stale data banner")
                    Spacer()
                    Button("Retry") { model.refresh() }
                        .fontWeight(.bold)
                        .foregroundStyle(RoameTheme.pink)
                }
                .padding(14)
                .background(RoameTheme.offline)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            } else if model.isError {
                VStack(alignment: .leading, spacing: 8) {
                    Text(model.errorMessage)
                        .accessibilityLabel("Error loading flights")
                    RoamePillButton(label: "Retry") { model.refresh() }
                }
                .padding(14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(RoameTheme.errorSoft)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            } else if model.isEmptyResponse {
                HStack {
                    Text("No flights returned from the server.")
                        .accessibilityLabel("Empty flights response")
                    Spacer()
                    Button("Retry") { model.refresh() }
                        .fontWeight(.bold)
                        .foregroundStyle(RoameTheme.pink)
                }
                .padding(14)
                .background(RoameTheme.pinkSoft)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            }
        }
    }

    private func banner(text: String, color: Color) -> some View {
        Text(text)
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(color)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

struct FlightRowView: View {
    let card: FlightCard
    let selected: Bool
    let saved: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 8) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(card.destinationCity)
                            .font(.system(.title3, design: .serif).weight(.semibold))
                            .foregroundStyle(RoameTheme.ink)
                        Text("\(card.origin) → \(card.destination)")
                            .font(.subheadline)
                            .foregroundStyle(RoameTheme.muted)
                    }
                    Spacer(minLength: 8)
                    Image(systemName: saved ? "bookmark.fill" : "bookmark")
                        .foregroundStyle(saved ? RoameTheme.pink : RoameTheme.muted)
                        .accessibilityLabel(saved ? "Saved flight" : "Not saved")
                }

                HStack(alignment: .firstTextBaseline) {
                    Text(card.points.formatted())
                        .font(.title2.bold())
                        .foregroundStyle(RoameTheme.ink)
                    Text("pts")
                        .font(.subheadline)
                        .foregroundStyle(RoameTheme.muted)
                    Spacer(minLength: 8)
                    Text(card.cabin)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(RoameTheme.pink)
                }

                HStack {
                    Text(card.departureDate)
                        .font(.caption)
                        .foregroundStyle(RoameTheme.muted)
                    Spacer()
                    Text(card.program)
                        .font(.caption.weight(.medium))
                        .foregroundStyle(RoameTheme.muted)
                }
            }
            .padding(14)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(selected ? RoameTheme.pinkSoft : RoameTheme.surface)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(selected ? RoameTheme.pink.opacity(0.35) : RoameTheme.hairline, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(
            "Flight \(card.origin) to \(card.destination) on \(card.departureDate), \(card.points) points"
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
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(flight.origin) → \(flight.destination)")
                        .font(.title3.bold())
                        .foregroundStyle(RoameTheme.ink)
                    Text("\(flight.originCity) → \(flight.destinationCity)")
                        .font(.caption)
                        .foregroundStyle(RoameTheme.muted)
                }
                Spacer()
                Button(action: onDismiss) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(RoameTheme.muted)
                }
                .accessibilityLabel("Clear selection")
            }
            Text("Depart \(flight.departureDate) · \(flight.cabin)")
                .foregroundStyle(RoameTheme.ink)
            Text(flight.program)
                .font(.caption)
                .foregroundStyle(RoameTheme.muted)
            Text("\(flight.points.formatted()) points")
                .font(.title3.bold())
            if saved {
                RoamePillButton(label: "Remove", filled: false, action: onRemove)
                    .accessibilityLabel("Remove saved flight")
            } else {
                RoamePillButton(label: "Save", action: onSave)
                    .accessibilityLabel("Save flight")
            }
        }
        .padding(16)
        .background(RoameTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(RoameTheme.hairline, lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.08), radius: 12, y: 4)
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
                    .font(.system(.headline, design: .serif))
                    .accessibilityLabel("Result count \(model.resultCount)")
                Spacer()
                if model.hasActiveFilters {
                    Button("Reset") { model.resetFilters() }
                        .fontWeight(.bold)
                        .foregroundStyle(RoameTheme.pink)
                }
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
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
                    if !model.datesAreDefault {
                        chip("\(model.dateFrom) → \(model.dateTo)") {
                            model.clearDateRange()
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
                .font(.caption.weight(.semibold))
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .foregroundStyle(RoameTheme.pinkDeep)
                .background(RoameTheme.pinkSoft)
                .overlay(Capsule().stroke(RoameTheme.pink.opacity(0.2), lineWidth: 1))
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}
