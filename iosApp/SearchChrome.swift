import SwiftUI
import Shared

@MainActor
enum SearchSummary {
    static func text(model: FlightExplorerModel) -> String {
        let origin = model.originText.trimmingCharacters(in: .whitespaces).isEmpty
            ? "Anywhere"
            : model.originText.trimmingCharacters(in: .whitespaces)
        let dest = model.destinationText.trimmingCharacters(in: .whitespaces).isEmpty
            ? "Anywhere"
            : model.destinationText.trimmingCharacters(in: .whitespaces)
        return "\(origin) → \(dest) · \(shortDate(model.dateFrom)) – \(shortDate(model.dateTo))"
    }

    static func detail(model: FlightExplorerModel) -> String {
        var parts: [String] = []
        if let cabin = model.cabin { parts.append(cabin) }
        if model.maxPointsEnabled {
            parts.append("≤ \(Int(model.maxPoints).formatted()) pts")
        }
        if !model.selectedPrograms.isEmpty {
            parts.append("\(model.selectedPrograms.count) programs")
        }
        return parts.isEmpty ? "Tap to edit filters" : parts.joined(separator: " · ")
    }

    private static func shortDate(_ iso: String) -> String {
        let parts = iso.split(separator: "-")
        guard parts.count == 3 else { return iso }
        let months = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"]
        let monthIdx = (Int(parts[1]) ?? 1) - 1
        let month = months[max(0, min(11, monthIdx))]
        return "\(month) \(Int(parts[2]) ?? 1)"
    }
}

struct SearchPill: View {
    let summary: String
    let detail: String
    let namespace: Namespace.ID
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                Image(systemName: "magnifyingglass")
                    .font(.body.weight(.semibold))
                    .foregroundStyle(RoameTheme.pink)
                VStack(alignment: .leading, spacing: 2) {
                    Text(summary)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(RoameTheme.ink)
                        .lineLimit(1)
                    Text(detail)
                        .font(.caption)
                        .foregroundStyle(RoameTheme.muted)
                        .lineLimit(1)
                }
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(RoameTheme.surface)
                    .matchedGeometryEffect(id: "searchCard", in: namespace)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .stroke(RoameTheme.hairline, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Open search filters")
    }
}

struct SearchPanel: View {
    @ObservedObject var model: FlightExplorerModel
    let namespace: Namespace.ID
    let onClose: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button(action: onClose) {
                    Image(systemName: "xmark")
                        .font(.body.weight(.semibold))
                        .foregroundStyle(RoameTheme.ink)
                        .frame(width: 36, height: 36)
                        .background(Circle().fill(Color(.systemGray6)))
                }
                .accessibilityLabel("Close search")
                Spacer()
                Text("Search flights")
                    .font(.system(.title3, design: .serif).weight(.semibold))
                Spacer()
                Color.clear.frame(width: 36, height: 36)
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 8)

            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    field(
                        title: "Origin",
                        text: Binding(
                            get: { model.originText },
                            set: { model.onOriginChanged($0) }
                        ),
                        placeholder: "Code or city",
                        a11y: "Origin search field"
                    )
                    field(
                        title: "Destination",
                        text: Binding(
                            get: { model.destinationText },
                            set: { model.onDestinationChanged($0) }
                        ),
                        placeholder: "Code or city",
                        a11y: "Destination search field"
                    )
                    HStack(spacing: 8) {
                        field(
                            title: "From date",
                            text: Binding(
                                get: { model.dateFrom },
                                set: { model.onDateFrom($0) }
                            ),
                            placeholder: "YYYY-MM-DD",
                            a11y: "Departure date from"
                        )
                        field(
                            title: "To date",
                            text: Binding(
                                get: { model.dateTo },
                                set: { model.onDateTo($0) }
                            ),
                            placeholder: "YYYY-MM-DD",
                            a11y: "Departure date to"
                        )
                    }

                    Text("Cabin")
                        .font(.subheadline.weight(.semibold))
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            RoameChip(label: "Any", selected: model.cabin == nil) {
                                model.onCabin(nil)
                            }
                            .accessibilityLabel("Cabin filter Any")
                            ForEach(["Economy", "Premium Economy", "Business"], id: \.self) { cabin in
                                RoameChip(label: cabin, selected: model.cabin == cabin) {
                                    model.onCabin(cabin)
                                }
                                .accessibilityLabel("Cabin filter \(cabin)")
                            }
                        }
                    }

                    Toggle(isOn: Binding(
                        get: { model.maxPointsEnabled },
                        set: { model.onMaxPointsChanged(model.maxPoints, enabled: $0) }
                    )) {
                        Text("Limit max points")
                    }
                    .tint(RoameTheme.pink)
                    .accessibilityLabel("Maximum points filter")

                    if model.maxPointsEnabled {
                        Text("Max points: \(Int(model.maxPoints).formatted())")
                            .foregroundStyle(RoameTheme.muted)
                        Slider(
                            value: Binding(
                                get: { model.maxPoints },
                                set: { model.onMaxPointsChanged($0, enabled: true) }
                            ),
                            in: 9_000...133_000,
                            step: 1_000
                        )
                        .tint(RoameTheme.pink)
                        .accessibilityLabel("Maximum points slider")
                    }

                    Text("Loyalty programs")
                        .font(.subheadline.weight(.semibold))
                    FlowLayout {
                        ForEach(LoyaltyPrograms.shared.all, id: \.self) { program in
                            let selected = model.selectedPrograms.contains(program)
                            RoameChip(label: program, selected: selected) {
                                model.toggleProgram(program)
                            }
                            .accessibilityLabel("Loyalty program \(program)")
                        }
                    }

                    Button("Reset filters") { model.resetFilters() }
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(RoameTheme.pink)
                }
                .padding(16)
            }

            RoamePillButton(label: "Search Flights", action: onClose)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .accessibilityLabel("Apply search")
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(RoameTheme.surface)
                .matchedGeometryEffect(id: "searchCard", in: namespace)
                .ignoresSafeArea(edges: .bottom)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(RoameTheme.hairline, lineWidth: 1)
                .ignoresSafeArea(edges: .bottom)
        )
    }

    private func field(
        title: String,
        text: Binding<String>,
        placeholder: String,
        a11y: String
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundStyle(RoameTheme.muted)
            TextField(placeholder, text: text)
                .padding(14)
                .background(RoameTheme.canvas)
                .overlay(Capsule().stroke(RoameTheme.hairline, lineWidth: 1))
                .clipShape(Capsule())
                .accessibilityLabel(a11y)
        }
    }
}
