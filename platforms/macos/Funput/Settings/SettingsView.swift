import SwiftUI

struct SettingsView: View {
    @Environment(AppSettings.self) private var settings
    @Environment(UpdaterManager.self) private var updater

    @State private var selection: SettingsDestination? = .overview
    @State private var presentation: SettingsPresentation?
    @State private var commands = SettingsCommandHandler()

    var body: some View {
        NavigationSplitView {
            SettingsSidebar(
                selection: $selection,
                onImport: importConfig,
                onExport: exportConfig,
                onAbout: showAbout
            )
        } detail: {
            destinationView
        }
        .tint(Theme.accent)
        .sheet(item: $presentation) { item in
            switch item {
            case .about:
                // No outer padding: the hero background now bleeds to the
                // sheet's edges instead of sitting inset inside a card.
                AboutPane()
                    .environment(settings)
                    .environment(updater)
                    .frame(width: 460)
            }
        }
        .alert(
            "Cấu hình Funput",
            isPresented: Binding(
                get: { commands.alertMessage != nil },
                set: { if !$0 { commands.alertMessage = nil } }
            )
        ) {
            Button("OK", role: .cancel) {}
        } message: {
            if let message = commands.alertMessage { Text(message) }
        }
    }

    @ViewBuilder private var destinationView: some View {
        switch selection ?? .overview {
        case .overview:
            OverviewPane(selection: $selection)
        case .typing:
            TypingPane()
        case .keyboardShortcuts:
            SettingsPage(destination: .keyboardShortcuts) {
                KeyboardPane()
            }
        case .textShortcuts:
            SettingsPage(destination: .textShortcuts) {
                ShortcutsPane()
            }
        case .applications:
            SettingsPage(destination: .applications) {
                AppExclusionPane()
            }
        }
    }

    private func importConfig() {
        commands.importConfig(into: settings)
    }

    private func exportConfig() {
        commands.exportConfig(from: settings)
    }

    private func showAbout() {
        presentation = .about
    }
}

#Preview {
    SettingsView()
        .environment(AppSettings.shared)
        .frame(width: Theme.settingsMinWidth, height: Theme.settingsMinHeight)
}
