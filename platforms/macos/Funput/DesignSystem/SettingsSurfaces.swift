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

/// A quick-stat card on the Overview pane. When `action` is provided, the whole
/// card becomes a button that jumps to the related settings pane (with a
/// trailing chevron as the tap affordance); otherwise it stays a plain,
/// non-interactive readout.
struct SettingsMetric: View {
    let title: String
    let value: String
    let systemImage: String
    var action: (() -> Void)? = nil

    var body: some View {
        if let action {
            Button(action: action) { card }
                .buttonStyle(.plain)
                .help("Mở \(title)")
        } else {
            card
        }
    }

    private var card: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            HStack {
                Image(systemName: systemImage)
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(.tint)
                if action != nil {
                    Spacer(minLength: Theme.Spacing.xs)
                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.tertiary)
                }
            }
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
