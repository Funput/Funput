import SwiftUI

struct SettingsView: View {
    @Environment(AppSettings.self) private var settings
    @Environment(UpdaterManager.self) private var updater

    @State private var selection: SettingsDestination? = .overview
    @State private var presentation: SettingsPresentation?
    @State private var commands = SettingsCommandHandler()

    var body: some View {
        NavigationSplitView {
            SettingsSidebar(selection: $selection)
        } detail: {
            destinationView
        }
        .tint(Theme.accent)
        .toolbar {
            SettingsToolbar(
                onImport: importConfig,
                onExport: exportConfig,
                onAbout: showAbout
            )
        }
        .sheet(item: $presentation) { item in
            switch item {
            case .about:
                AboutPane()
                    .environment(settings)
                    .environment(updater)
                    .frame(width: 460)
                    .padding(Theme.Spacing.xl)
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
            OverviewPane()
        case .typing:
            TypingPane()
        case .automation:
            AutomationPane()
        case .shortcuts:
            SettingsPage(destination: .shortcuts) {
                KeyboardPane()
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
