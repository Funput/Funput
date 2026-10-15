import SwiftUI

struct SettingsSelectionRow: View {
    let option: SettingsPicker
    let value: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            SettingsRowLabel(
                title: option.title,
                summary: option.summary,
                systemImage: option.systemImage,
                value: value
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(option.title), \(value)")
        .accessibilityHint("Chạm hai lần để thay đổi")
    }
}

/// The label every row that leads somewhere shares: icon, text column, current value
/// and a chevron, so pickers and page links line up in one card.
struct SettingsRowLabel: View {
    let title: String
    var summary: String?
    let systemImage: String
    var value: String?

    var body: some View {
        HStack(spacing: 12) {
            SettingsRowIcon(systemImage: systemImage)
            VStack(alignment: .leading, spacing: 3) {
                Text(title).foregroundStyle(.primary)
                if let summary {
                    Text(summary).font(.caption).foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: 12)
            if let value {
                Text(value).font(.subheadline).foregroundStyle(.secondary)
            }
            Image(systemName: "chevron.right").font(.caption.bold()).foregroundStyle(.tertiary)
        }
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }
}

struct SettingsToggleRow: View {
    let title: String
    let summary: String
    var systemImage: String?
    @Binding var isOn: Bool

    var body: some View {
        Toggle(isOn: $isOn) {
            HStack(spacing: 12) {
                if let systemImage {
                    SettingsRowIcon(systemImage: systemImage)
                }
                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                    Text(summary).font(.caption).foregroundStyle(.secondary)
                }
            }
        }
        .tint(.accentColor)
        .padding(.vertical, 11)
        .accessibilityHint(summary)
    }
}

struct SettingsHeightRow: View {
    @Binding var value: Double

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 12) {
                SettingsRowIcon(systemImage: "arrow.up.and.down")
                Text("Chiều cao bàn phím")
                Spacer()
                Text(value, format: .percent.precision(.fractionLength(0)))
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
            }
            Slider(value: $value, in: 0.85...1.2, step: 0.01) {
                Text("Chiều cao bàn phím")
            } minimumValueLabel: {
                Text("85%").font(.caption2)
            } maximumValueLabel: {
                Text("120%").font(.caption2)
            }
            .tint(.accentColor)
            .padding(.leading, 34)
        }
        .padding(.vertical, 12)
        .accessibilityElement(children: .contain)
    }
}

/// The shared leading column, so selection, toggle and link rows start text at one edge.
struct SettingsRowIcon: View {
    let systemImage: String
    var tint: Color?

    var body: some View {
        Image(systemName: systemImage)
            .foregroundStyle(tint.map(AnyShapeStyle.init) ?? AnyShapeStyle(.tint))
            .frame(width: 22)
            .accessibilityHidden(true)
    }
}
