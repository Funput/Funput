import SwiftUI
import UIKit

/// The frame every settings page shares: the app's glass screen, its title, and the
/// picker sheet and save alert its rows can raise.
///
/// Each page presents its own sheet and alert, so nothing has to be presented from the
/// keyboard tab's root while a pushed page covers it.
struct SettingsPage<Content: View>: View {
    let destination: SettingsDestination
    @ObservedObject var model: SettingsModel
    private let content: (_ select: @escaping (SettingsPicker) -> Void) -> Content
    @State private var picker: SettingsPicker?

    init(
        _ destination: SettingsDestination,
        model: SettingsModel,
        @ViewBuilder content: @escaping (_ select: @escaping (SettingsPicker) -> Void) -> Content
    ) {
        self.destination = destination
        self.model = model
        self.content = content
    }

    var body: some View {
        AppScreen {
            content { picker = $0 }
        }
        .environment(\.settingsPageTitle, destination.title)
        .navigationTitle(destination.title)
        .navigationBarTitleDisplayMode(.inline)
        .sheet(item: $picker) { SettingsSelectionSheet(picker: $0, model: model) }
        .alert("Không thể lưu cài đặt", isPresented: model.saveErrorBinding) {
            Button("Đóng", role: .cancel) {}
        } message: {
            Text("Funput đã giữ nguyên giá trị trước đó. Vui lòng thử lại.")
        }
    }
}

extension URL {
    /// Funput's page in the system Settings app, where the keyboard and its permissions live.
    static let funputSystemSettings = URL(string: UIApplication.openSettingsURLString)!
}
