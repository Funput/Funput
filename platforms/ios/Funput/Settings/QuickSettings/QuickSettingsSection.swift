import FunputShared
import KeyboardLayout
import SwiftUI

/// The top of the keyboard tab: the input method, then the switches people flip most.
struct QuickSettingsSection: View {
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ObservedObject var model: SettingsModel
    @State private var pendingAccess: FullAccessFeature?

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            SettingsGroupHeader(title: "Kiểu gõ")
            // A wrong method ("chaof" instead of "chào") is the first reason people open
            // these settings, so it is the one choice kept on the tab itself.
            Picker("Kiểu gõ", selection: inputMethod) {
                ForEach(KeyboardInputMethod.allCases, id: \.self) { method in
                    Text(method.settingsTitle).tag(method)
                }
            }
            .pickerStyle(.segmented)
            .accessibilityIdentifier("settings.quick.inputMethod")
            SettingsGroupHeader(title: "Cài đặt nhanh")
                .padding(.top, 6)
            // A plain grid, not a lazy one: every glass tile stays mounted (see AppScreen).
            Grid(horizontalSpacing: 10, verticalSpacing: 10) {
                ForEach(rows.indices, id: \.self) { row in
                    GridRow {
                        ForEach(rows[row]) { setting in tile(setting) }
                    }
                }
            }
        }
        .fullAccessPrompt($pendingAccess, model: model)
    }

    /// Three across; two once text is large enough that three would squeeze the names.
    private var rows: [[QuickSetting]] {
        let columns = dynamicTypeSize.isAccessibilitySize ? 2 : 3
        return stride(from: 0, to: QuickSetting.allCases.count, by: columns).map { start in
            Array(QuickSetting.allCases[start..<min(start + columns, QuickSetting.allCases.count)])
        }
    }

    private var inputMethod: Binding<KeyboardInputMethod> {
        Binding(get: { model.configuration.inputMethod }, set: { model.update(\.inputMethod, to: $0) })
    }

    private func tile(_ setting: QuickSetting) -> some View {
        QuickSettingTile(
            setting: setting,
            isOn: model.isOn(setting),
            isLocked: model.isLocked(setting),
            state: model.stateLabel(for: setting)
        ) {
            pendingAccess = model.toggle(setting)
        }
    }
}
