#if DEBUG
import FunputShared
import KeyboardLayout
import SwiftUI
import UIKit

/// Only reachable through an explicit UI-test launch argument.
struct ShortcutsKeyboardHarness: View {
    @State private var model = ShortcutsModel(store: ShortcutsStoreFactory.make())

    var body: some View {
        NavigationStack {
            ShortcutsScreen(model: model)
                .toolbar {
                    ToolbarItem(placement: .bottomBar) {
                        NavigationLink("Mở ô nhập") { ShortcutHarnessFields() }
                            .accessibilityIdentifier("shortcuts.test.openField")
                    }
                }
        }
        .onAppear {
            TypingHarnessView.forceDeterministicConfiguration()
            if ProcessInfo.processInfo.environment["FUNPUT_SHORTCUTS_TEST_ENGLISH"] == "1" {
                let store = FunputUITestConfigurationOverrideStore()
                if var configuration = store.load() {
                    configuration.language = .english
                    store.save(configuration, expiresAt: Date().addingTimeInterval(600))
                }
            }
        }
    }
}

private struct ShortcutHarnessFields: View {
    @State private var field = "Text"
    var body: some View {
        VStack {
            Picker("Ô nhập", selection: $field) {
                ForEach(["Text", "Search", "Email", "URL"], id: \.self) { Text($0) }
            }
            .pickerStyle(.segmented)
            ShortcutHarnessTextView(kind: field).id(field)
        }
        .navigationTitle("Kiểm thử bàn phím")
        .toolbar {
            ToolbarItem(placement: .bottomBar) {
                Button("Đóng bàn phím") {
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                }
            }
        }
    }
}

private struct ShortcutHarnessTextView: UIViewRepresentable {
    let kind: String
    func makeUIView(context: Context) -> UITextView {
        let view = UITextView()
        view.accessibilityIdentifier = "typingHarness.field"
        view.font = .preferredFont(forTextStyle: .title3)
        view.adjustsFontForContentSizeCategory = true
        view.autocorrectionType = .no
        view.autocapitalizationType = .none
        view.spellCheckingType = .no
        view.smartQuotesType = .no
        view.smartDashesType = .no
        switch kind {
        case "Search": view.keyboardType = .webSearch
        case "Email": view.keyboardType = .emailAddress
        case "URL": view.keyboardType = .URL
        default: view.keyboardType = .default
        }
        return view
    }
    func updateUIView(_ view: UITextView, context: Context) {}
}
#endif
