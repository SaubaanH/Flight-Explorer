import SwiftUI

enum RoameTheme {
    static let pink = Color(red: 0.91, green: 0.23, blue: 0.44) // #E83A6F
    static let pinkSoft = Color(red: 1.0, green: 0.90, blue: 0.93)
    static let pinkDeep = Color(red: 0.79, green: 0.09, blue: 0.29)
    static let coral = Color(red: 1.0, green: 0.55, blue: 0.36)
    static let ink = Color(red: 0.10, green: 0.10, blue: 0.10)
    static let muted = Color(red: 0.42, green: 0.42, blue: 0.42)
    static let hairline = Color(red: 0.91, green: 0.90, blue: 0.89)
    static let canvas = Color(red: 0.96, green: 0.96, blue: 0.95)
    static let surface = Color.white
    static let offline = Color(red: 1.0, green: 0.95, blue: 0.88)
    static let errorSoft = Color(red: 1.0, green: 0.91, blue: 0.93)

    static var ctaGradient: LinearGradient {
        LinearGradient(colors: [coral, pink], startPoint: .leading, endPoint: .trailing)
    }
}

struct RoameBrandHeader: View {
    let title: String
    var subtitle: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("roame")
                .font(.system(size: 22, weight: .bold, design: .rounded))
                .foregroundStyle(RoameTheme.pink)
                .tracking(-0.5)
            Text(title)
                .font(.system(.title2, design: .serif).weight(.semibold))
                .foregroundStyle(RoameTheme.ink)
            if let subtitle {
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundStyle(RoameTheme.muted)
            }
        }
    }
}

struct RoamePillButton: View {
    let label: String
    var filled: Bool = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.subheadline.weight(.semibold))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .foregroundStyle(filled ? Color.white : RoameTheme.pink)
                .background(filled ? RoameTheme.pink : Color.clear)
                .overlay(
                    Capsule().stroke(RoameTheme.pink, lineWidth: filled ? 0 : 1)
                )
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

struct RoameChip: View {
    let label: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.caption.weight(.semibold))
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .foregroundStyle(selected ? Color.white : RoameTheme.ink)
                .background(selected ? RoameTheme.pink : RoameTheme.surface)
                .overlay(
                    Capsule().stroke(selected ? RoameTheme.pink : RoameTheme.hairline, lineWidth: 1)
                )
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}
