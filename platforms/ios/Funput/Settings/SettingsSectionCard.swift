import SwiftUI

struct SettingsSectionCard<Content: View>: View {
    let title: String
    let systemImage: String
    let footer: String?
    let content: Content

    init(
        title: String,
        systemImage: String,
        footer: String? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.systemImage = systemImage
        self.footer = footer
        self.content = content()
    }

    @Environment(\.settingsPageTitle) private var pageTitle

    var body: some View {
        ContentCard {
            // On a page of the same name the navigation bar already says it.
            if title != pageTitle {
                Label(title, systemImage: systemImage)
                    .font(.headline)
            }
            VStack(spacing: 0) {
                content
            }
            if let footer {
                Text(footer)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

/// Inset to the text column of a row: a 22pt icon plus the 12pt row spacing.
struct SettingsRowDivider: View {
    var body: some View {
        Divider().padding(.leading, 34)
    }
}

extension View {
    /// Liquid Glass for the screen's action buttons on iOS 26, bordered before it.
    /// Cards stay passive (see ``ContentCard``); only the controls themselves are glass.
    @ViewBuilder func settingsActionButtonStyle(prominent: Bool = false) -> some View {
        if #available(iOS 26, *) {
            if prominent {
                buttonStyle(.glassProminent)
            } else {
                buttonStyle(.glass)
            }
        } else if prominent {
            buttonStyle(.borderedProminent)
        } else {
            buttonStyle(.bordered)
        }
    }
}

private struct SettingsPageTitleKey: EnvironmentKey {
    static let defaultValue: String? = nil
}

extension EnvironmentValues {
    /// The title of the settings page a card sits on, set by ``SettingsPage``.
    var settingsPageTitle: String? {
        get { self[SettingsPageTitleKey.self] }
        set { self[SettingsPageTitleKey.self] = newValue }
    }
}
