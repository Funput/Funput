import Combine
import FunputShared
import SwiftUI
import ThemeRuntime
import UIKit

struct SettingsScreen: View {
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.openURL) private var openURL
    @StateObject private var model: SettingsModel
    @State private var picker: SettingsPicker?
    @State private var requestsHapticAccess = false
    @State private var requestsSoundAccess = false
    @StateObject private var shortcuts = ShortcutsModel(store: ShortcutsStoreFactory.make())

    init(
        store: any FunputConfigurationStoring = FunputConfigurationStore(),
        customStore: any CustomThemeStoring = CustomThemeStore(),
        bootstrap: any KeyboardBootstrapSynchronizing = KeyboardBootstrapSynchronizer()
    ) {
#if DEBUG
        if ShortcutsStoreFactory.isUITesting {
            _model = StateObject(wrappedValue: SettingsModel(
                store: PreviewConfigurationStore(),
                customStore: PreviewCustomThemeStore(),
                bootstrap: NoopKeyboardBootstrapSynchronizer()
            ))
            return
        }
#endif
        _model = StateObject(wrappedValue: SettingsModel(
            store: store, customStore: customStore, bootstrap: bootstrap
        ))
    }

    var body: some View {
        AppScreen {
            KeyboardSetupCard(hasFullAccess: model.hasFullAccess) {
                openURL(URL(string: UIApplication.openSettingsURLString)!)
            }
            TypingSettingsSection(model: model, shortcuts: shortcuts) { picker = $0 }
            LayoutSettingsSection(model: model) { picker = $0 }
            SmartInputSettingsSection(model: model)
            FeedbackSettingsCard(
                haptics: model.fullAccessBinding(\.isHapticFeedbackEnabled) {
                    requestsHapticAccess = true
                },
                sound: model.fullAccessBinding(\.isKeySoundEnabled) {
                    requestsSoundAccess = true
                },
                keyPreviews: model.boolBinding(\.showsKeyPreviews)
            )
            ClipboardSettingsCard(
                isEnabled: model.boolBinding(\.clipboardEnabled),
                expiryLabel: model.clipboardExpiryLabel,
                selectExpiry: { picker = .clipboardExpiry },
                openSettings: { openURL(URL(string: UIApplication.openSettingsURLString)!) }
            )
            SettingsDataCard(
                resetPersonalSuggestions: { model.requestPersonalSuggestionReset() },
                clearClipboard: { model.clearClipboardHistory() },
                resetSettings: { model.reset() }
            )
        }
        .navigationTitle("Cài đặt")
        .sheet(item: $picker) { SettingsSelectionSheet(picker: $0, model: model) }
        .alert("Không thể lưu cài đặt", isPresented: model.saveErrorBinding) {
            Button("Đóng", role: .cancel) {}
        } message: {
            Text("Funput đã giữ nguyên giá trị trước đó. Vui lòng thử lại.")
        }
        .alert("Cho phép truy cập đầy đủ", isPresented: $requestsHapticAccess) {
            Button("Hủy", role: .cancel) {}
            Button("Mở Cài đặt") {
                model.update(\.isHapticFeedbackEnabled, to: true)
                openURL(URL(string: UIApplication.openSettingsURLString)!)
            }
        } message: {
            Text("Để rung hoạt động, hãy mở Funput trong Cài đặt, chọn Bàn phím và bật Cho phép truy cập đầy đủ.")
        }
        .alert("Cho phép truy cập đầy đủ", isPresented: $requestsSoundAccess) {
            Button("Hủy", role: .cancel) {}
            Button("Mở Cài đặt") {
                model.update(\.isKeySoundEnabled, to: true)
                openURL(URL(string: UIApplication.openSettingsURLString)!)
            }
        } message: {
            Text("Để phát âm thanh khi gõ, hãy mở Funput trong Cài đặt, chọn Bàn phím và bật Cho phép truy cập đầy đủ.")
        }
        .background {
            if #available(iOS 17, *) {
                Color.clear.onChange(of: scenePhase) { _, p in if p == .active { model.reload() } }
            } else {
                Color.clear.onChange(of: scenePhase) { p in if p == .active { model.reload() } }
            }
        }
    }
}
