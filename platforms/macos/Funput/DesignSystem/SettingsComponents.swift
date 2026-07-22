import SwiftUI

/// A labelled settings row with its control aligned on the trailing edge.
struct SettingsRow<Control: View>: View {
    let title: String
    var subtitle: String? = nil
    var systemImage: String? = nil
    @ViewBuilder var control: Control

    var body: some View {
        HStack(alignment: .center, spacing: Theme.Spacing.md) {
            if let systemImage {
                Image(systemName: systemImage)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(.tint)
                    .frame(width: 22)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.body)
                if let subtitle {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: Theme.Spacing.md)
            control
        }
        .padding(.vertical, Theme.Spacing.xs)
    }
}

struct SectionHeader: View {
    let title: String

    var body: some View {
        Text(title)
            .font(.headline)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// A keyboard keycap in the content layer.
struct KeyCap: View {
    let label: String

    var body: some View {
        Text(label)
            .font(.system(size: 12, weight: .semibold, design: .rounded))
            .monospacedDigit()
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(.quaternary, in: RoundedRectangle(cornerRadius: 6, style: .continuous))
    }
}

struct ShortcutCaps: View {
    let caps: [String]

    var body: some View {
        HStack(spacing: Theme.Spacing.xs) {
            ForEach(caps, id: \.self) { KeyCap(label: $0) }
        }
    }
}

#Preview("Settings components") {
    VStack(alignment: .leading, spacing: Theme.Spacing.lg) {
        SectionHeader(title: "Preview")
        SettingsSurface {
            VStack(spacing: Theme.Spacing.sm) {
                SettingsRow(title: "Một tuỳ chọn", subtitle: "Mô tả ngắn", systemImage: "sparkles") {
                    Toggle("Một tuỳ chọn", isOn: .constant(true))
                        .labelsHidden()
                        .toggleStyle(.switch)
                        .tint(Theme.accent)
                }
                SettingsRow(title: "Phím chuyển") {
                    ShortcutCaps(caps: ["⌃", "\\"])
                }
            }
        }
    }
    .padding(Theme.Spacing.xl)
    .frame(width: 460)
}
