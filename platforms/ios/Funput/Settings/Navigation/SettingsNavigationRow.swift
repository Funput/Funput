import SwiftUI

/// A row that opens a settings page, with a sentence on where that page stands.
struct SettingsNavigationRow: View {
    let destination: SettingsDestination
    let summary: String?

    var body: some View {
        NavigationLink(value: destination) {
            SettingsRowLabel(
                title: destination.title,
                summary: summary,
                systemImage: destination.systemImage
            )
        }
        .buttonStyle(SettingsRowButtonStyle())
        .accessibilityIdentifier("settings.destination.\(destination.rawValue)")
    }
}

/// Plain rows inside a passive card: the press is shown by a light wash rather than
/// glass, so a card of rows stays one surface instead of one glass pass per row.
struct SettingsRowButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(
                configuration.isPressed ? AnyShapeStyle(.quaternary) : AnyShapeStyle(.clear),
                in: RoundedRectangle(cornerRadius: 10, style: .continuous)
            )
            .contentShape(Rectangle())
    }
}

/// A card of page links, each describing its page's current state.
struct SettingsNavigationCard: View {
    let destinations: [SettingsDestination]
    @ObservedObject var model: SettingsModel

    var body: some View {
        ContentCard {
            VStack(spacing: 0) {
                ForEach(destinations) { destination in
                    if destination != destinations.first {
                        SettingsRowDivider()
                    }
                    SettingsNavigationRow(destination: destination, summary: model.summary(for: destination))
                }
            }
        }
    }
}
