import AppKit
import SwiftUI

struct MenuBarActionCluster: View {
    @Environment(UpdaterManager.self) private var updater
    @Environment(\.openWindow) private var openWindow

    var body: some View {
        GlassEffectContainer(spacing: Theme.Spacing.sm) {
            HStack(spacing: Theme.Spacing.sm) {
                actionButton("Cài đặt", systemImage: "gearshape") {
                    open(WindowID.settings)
                }
                actionButton("Hướng dẫn", systemImage: "sparkles.rectangle.stack") {
                    open(WindowID.onboarding)
                }
                moreMenu
            }
        }
    }

    private var moreMenu: some View {
        Menu {
            Button("Kiểm tra cập nhật…", systemImage: "arrow.triangle.2.circlepath") {
                updater.checkForUpdates()
            }
            .disabled(!updater.canCheckForUpdates)
            Divider()
            Button("Thoát Funput", systemImage: "power") {
                NSApp.terminate(nil)
            }
            .keyboardShortcut("q", modifiers: .command)
        } label: {
            Label("Thêm", systemImage: "ellipsis")
                .frame(maxWidth: .infinity)
        }
        .menuStyle(.button)
        .buttonStyle(.glass)
    }

    private func actionButton(
        _ title: String,
        systemImage: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Label(title, systemImage: systemImage)
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.glass)
    }

    private func open(_ id: String) {
        openWindow(id: id)
        NSApp.activate(ignoringOtherApps: true)
    }
}
