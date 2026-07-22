import AppKit
import SwiftUI

struct SettingsSurface<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        content
            .padding(Theme.Spacing.lg)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
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
        ScrollView {
            VStack(alignment: .leading, spacing: Theme.Spacing.xl) {
                CompactPageHeader(destination: destination)
                content
            }
            .padding(Theme.Spacing.xl)
            .frame(maxWidth: Theme.contentMaxWidth, alignment: .leading)
            .frame(maxWidth: .infinity, alignment: .center)
        }
        .background(.windowBackground)
        .navigationTitle(destination.title)
    }
}

struct CompactPageHeader: View {
    let destination: SettingsDestination

    var body: some View {
        HStack(spacing: Theme.Spacing.md) {
            Image(systemName: destination.systemImage)
                .font(.system(size: 21, weight: .semibold))
                .foregroundStyle(.tint)
                .frame(width: 44, height: 44)
                .background(.tint.opacity(0.12), in: .circle)

            VStack(alignment: .leading, spacing: 2) {
                Text(destination.title)
                    .font(.title2.bold())
                Text(destination.subtitle)
                    .font(.callout)
                    .foregroundStyle(.secondary)
            }
        }
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
