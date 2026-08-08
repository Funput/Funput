import SwiftUI

struct OverviewPane: View {
    @Environment(AppSettings.self) private var settings
    /// Lets the metric cards jump to their related sidebar destination.
    @Binding var selection: SettingsDestination?

    var body: some View {
        @Bindable var settings = settings

        ScrollView {
            LazyVStack(spacing: 0) {
                OverviewHero()

                VStack(alignment: .leading, spacing: Theme.Spacing.lg) {
                    HStack(spacing: Theme.Spacing.md) {
                        // "Phương thức" has no dedicated page of its own (the
                        // actual picker lives in the sidebar's quick control),
                        // so this links to the closest related pane instead.
                        SettingsMetric(
                            title: "Phương thức",
                            value: settings.inputMethod.displayName,
                            systemImage: "keyboard"
                        ) {
                            selection = .typing
                        }
                        SettingsMetric(
                            title: "Gõ tắt",
                            value: "\(settings.shortcuts.count)",
                            systemImage: "text.append"
                        ) {
                            selection = .textShortcuts
                        }
                    }

                    SettingsSurface {
                        VStack(spacing: Theme.Spacing.md) {
                            SettingsRow(
                                title: "Khởi động cùng máy",
                                subtitle: "Tự chạy Funput khi bạn đăng nhập",
                                systemImage: "power"
                            ) {
                                Toggle("Khởi động cùng máy", isOn: $settings.launchAtLogin)
                                    .labelsHidden()
                                    .toggleStyle(.switch)
                                    .tint(Theme.accent)
                                    .onChange(of: settings.launchAtLogin) { _, enabled in
                                        LoginItemController.setEnabled(enabled)
                                    }
                            }

                            Divider()

                            SettingsRow(
                                title: "Hiện biểu tượng thanh menu",
                                subtitle: "Mở nhanh Control Center từ thanh menu",
                                systemImage: "menubar.rectangle"
                            ) {
                                Toggle("Hiện biểu tượng thanh menu", isOn: $settings.showMenuBarIcon)
                                    .labelsHidden()
                                    .toggleStyle(.switch)
                                    .tint(Theme.accent)
                            }
                        }
                    }
                }
                .padding(Theme.Spacing.xl)
                .frame(maxWidth: Theme.contentMaxWidth)
                .frame(maxWidth: .infinity)
            }
        }
        .background(.windowBackground)
        .navigationTitle(SettingsDestination.overview.title)
    }
}

#Preview {
    @Previewable @State var selection: SettingsDestination? = .overview
    OverviewPane(selection: $selection)
        .environment(AppSettings.shared)
        .frame(width: 740, height: 680)
}
