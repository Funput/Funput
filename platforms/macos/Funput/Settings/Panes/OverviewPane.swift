import SwiftUI

struct OverviewPane: View {
    @Environment(AppSettings.self) private var settings

    var body: some View {
        @Bindable var settings = settings

        ScrollView {
            LazyVStack(spacing: 0) {
                OverviewHero()

                VStack(alignment: .leading, spacing: Theme.Spacing.lg) {
                    HStack(spacing: Theme.Spacing.md) {
                        SettingsMetric(
                            title: "Phương thức",
                            value: settings.inputMethod.displayName,
                            systemImage: "keyboard"
                        )
                        SettingsMetric(
                            title: "Gõ tắt",
                            value: "\(settings.shortcuts.count)",
                            systemImage: "text.append"
                        )
                        SettingsMetric(
                            title: "Ứng dụng bỏ qua",
                            value: "\(settings.excludedApps.count)",
                            systemImage: "app.badge"
                        )
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
    OverviewPane()
        .environment(AppSettings.shared)
        .frame(width: 740, height: 680)
}
