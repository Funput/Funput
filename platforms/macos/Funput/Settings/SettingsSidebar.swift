import SwiftUI

struct SettingsSidebar: View {
    @Binding var selection: SettingsDestination?
    @Environment(UpdaterManager.self) private var updater

    let onImport: () -> Void
    let onExport: () -> Void
    let onAbout: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            navigation
            Divider()
                .padding(.horizontal, Theme.Spacing.md)
            SidebarQuickInputControl()
                .padding(Theme.Spacing.md)
        }
        .navigationSplitViewColumnWidth(Theme.sidebarWidth)
    }

    private var navigation: some View {
        List(selection: $selection) {
            destinations(SettingsDestination.general)

            Section("Gõ tiếng Việt") {
                destinations(SettingsDestination.vietnameseTyping)
            }

            Section("Tự động hóa") {
                destinations(SettingsDestination.automation)
            }

            Section("Công cụ") {
                HStack {
                    // Leading icon matches the Label(icon + text) rhythm used by
                    // the navigation destinations() rows above, instead of a
                    // bare Text that looked out of place in the sidebar.
                    Label("Dữ liệu", systemImage: "tray.full")
                    Spacer()
                    ControlGroup {
                        utilityButton(
                            "Nhập cấu hình",
                            systemImage: "square.and.arrow.down",
                            perform: onImport
                        )
                        utilityButton(
                            "Xuất cấu hình",
                            systemImage: "square.and.arrow.up",
                            perform: onExport
                        )
                    }
                }

                HStack {
                    // Use the actual Funput brand mark instead of a generic SF
                    // Symbol here to emphasize brand identity for this row.
                    Label {
                        Text("Funput")
                    } icon: {
                        Image("FunputBrand")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 16, height: 16)
                    }
                    Spacer()
                    ControlGroup {
                        utilityButton(
                            "Kiểm tra cập nhật",
                            systemImage: "arrow.triangle.2.circlepath"
                        ) {
                            updater.checkForUpdates()
                        }
                        .disabled(!updater.canCheckForUpdates)
                        utilityButton(
                            "Giới thiệu Funput",
                            systemImage: "info.circle",
                            perform: onAbout
                        )
                    }
                }
            }
        }
        .listStyle(.sidebar)
    }

    @ViewBuilder
    private func destinations(_ items: [SettingsDestination]) -> some View {
        ForEach(items) { destination in
            Label(destination.title, systemImage: destination.systemImage)
                .tag(destination)
        }
    }

    private func utilityButton(
        _ title: String,
        systemImage: String,
        perform: @escaping () -> Void
    ) -> some View {
        Button(action: perform) {
            Image(systemName: systemImage)
        }
        .help(title)
        .accessibilityLabel(title)
    }
}

private struct SidebarQuickInputControl: View {
    @Environment(AppSettings.self) private var settings

    var body: some View {
        @Bindable var settings = settings

        VStack(alignment: .leading, spacing: Theme.Spacing.md) {
            Text("Điều khiển nhanh")
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
                .textCase(.uppercase)

            HStack(spacing: Theme.Spacing.sm) {
                Picker("Kiểu gõ", selection: $settings.inputMethod) {
                    ForEach(InputMethod.displayCases) { method in
                        Text(method.displayName).tag(method)
                    }
                }
                .pickerStyle(.menu)
                .labelsHidden()
                // Override the ambient `.tint(Theme.accent)` set by SettingsView so the
                // menu's text/chevron stay neutral instead of inheriting the app accent.
                .tint(.primary)
                // Idle (unclicked) state gets a Liquid Glass surface; `.interactive()`
                // lets the system add the press/hover response when the menu opens.
                // No vertical padding: let the control keep its intrinsic (compact)
                // height instead of matching the segmented "Chế độ nhập" control.
                .padding(.horizontal, Theme.Spacing.sm)
                .glassEffect(.regular.interactive(), in: .rect(cornerRadius: Theme.Radius.control))
                .accessibilityLabel("Kiểu gõ")
                .accessibilityValue(settings.inputMethod.displayName)

                // Custom Liquid Glass toggle instead of Picker(.segmented): the native
                // segmented control only tints its selected segment while the window
                // is key/active, so `Theme.accent` wouldn't show reliably here.
                // Route writes through `setVietnameseFromUI` so the choice binds to
                // the app the user returns to (not lost to the per-app default).
                GlassLanguageToggle(
                    isVietnameseEnabled: Binding(
                        get: { settings.vietnameseEnabled },
                        set: { settings.setVietnameseFromUI($0) }
                    )
                )
            }
        }
    }
}
