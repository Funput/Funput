import SwiftUI

struct SettingsSurface<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        content
            .padding(Theme.Spacing.lg)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                .quaternary.opacity(0.45),
                in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous)
            )
            .overlay {
                RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous)
                    .strokeBorder(.separator.opacity(0.28), lineWidth: 0.5)
            }
    }
}

struct SettingsPage<Content: View>: View {
    let destination: SettingsDestination
    @ViewBuilder var content: Content

    var body: some View {
        Form {
            content
        }
        .formStyle(.grouped)
        .frame(maxWidth: Theme.contentMaxWidth)
        .frame(maxWidth: .infinity)
        .background(.windowBackground)
        .navigationTitle(destination.title)
    }
}

struct SettingsMetric: View {
    let title: String
    let value: String
    let systemImage: String

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            Image(systemName: systemImage)
                .font(.title3.weight(.semibold))
                .foregroundStyle(.tint)
            Text(value)
                .font(.title3.bold())
                .lineLimit(1)
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Theme.Spacing.md)
        .background(.quaternary.opacity(0.65), in: RoundedRectangle(cornerRadius: Theme.Radius.control, style: .continuous))
    }
}
