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

private struct SettingsSidebar: View {
    @Binding var selection: SettingsDestination?

    var body: some View {
        VStack(spacing: 0) {
            SidebarBrandHeader()
                .padding(.horizontal, Theme.Spacing.md)
                .padding(.top, Theme.Spacing.sm)
                .padding(.bottom, Theme.Spacing.md)

            Divider()
                .padding(.horizontal, Theme.Spacing.md)

            List(selection: $selection) {
                ForEach(SettingsDestination.allCases) { destination in
                    Label(destination.title, systemImage: destination.systemImage)
                        .tag(destination)
                }
            }
            .listStyle(.sidebar)
        }
        .navigationSplitViewColumnWidth(Theme.sidebarWidth)
    }
}

private struct SidebarBrandHeader: View {
    var body: some View {
        HStack(spacing: Theme.Spacing.md) {
            AppLogo(size: 44, contentPadding: 0)

            Text("Funput")
                .font(.system(size: 24, weight: .bold, design: .rounded))
                .foregroundStyle(.primary)
        }
        .padding(.horizontal, Theme.Spacing.md)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .center)
        .background(
            .regularMaterial,
            in: RoundedRectangle(cornerRadius: Theme.Radius.control, style: .continuous)
        )
        .overlay {
            RoundedRectangle(cornerRadius: Theme.Radius.control, style: .continuous)
                .strokeBorder(.white.opacity(0.08), lineWidth: 0.5)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Funput")
    }
}

private struct SettingsToolbar: ToolbarContent {
    @Environment(AppSettings.self) private var settings
    @Environment(UpdaterManager.self) private var updater

    let onImport: () -> Void
    let onExport: () -> Void
    let onAbout: () -> Void

    var body: some ToolbarContent {
        ToolbarItemGroup(placement: .primaryAction) {
            Button(action: toggleVietnamese) {
                Label(
                    settings.vietnameseEnabled ? "VI" : "EN",
                    systemImage: settings.vietnameseEnabled ? "checkmark.circle.fill" : "pause.circle"
                )
            }
            .help(settings.vietnameseEnabled ? "Tạm dừng tiếng Việt" : "Bật tiếng Việt")

            Menu {
                @Bindable var settings = settings
                Picker("Phương thức gõ", selection: $settings.inputMethod) {
                    ForEach(InputMethod.displayCases) { method in
                        Text(method.displayName).tag(method)
                    }
                }
            } label: {
                Label(settings.inputMethod.displayName, systemImage: "keyboard")
            }

            Menu {
                Button("Nhập cấu hình…", systemImage: "square.and.arrow.down", action: onImport)
                Button("Xuất cấu hình…", systemImage: "square.and.arrow.up", action: onExport)
                Divider()
                Button("Kiểm tra cập nhật…", systemImage: "arrow.triangle.2.circlepath") {
                    updater.checkForUpdates()
                }
                .disabled(!updater.canCheckForUpdates)
                Button("Giới thiệu Funput", systemImage: "info.circle", action: onAbout)
            } label: {
                Label("Thêm", systemImage: "ellipsis")
            }
        }
    }

    private func toggleVietnamese() {
        settings.vietnameseEnabled.toggle()
    }
}

#Preview {
    SettingsView()
        .environment(AppSettings.shared)
        .frame(width: Theme.settingsMinWidth, height: Theme.settingsMinHeight)
}
