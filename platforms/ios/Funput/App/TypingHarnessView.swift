import FunputShared
import KeyboardLayout
import SwiftUI
import UIKit

/// Full-screen text view shown instead of the normal app when launched with
/// the `-uitest-typing-harness` argument (see `ContentView`). Gives UI tests a
/// plain, smart-feature-free target to type into with the Funput keyboard
/// extension, so an end-to-end typing run (key taps → extension → committed
/// text) can be asserted character-for-character.
struct TypingHarnessView: View {
    var body: some View {
        HarnessTextView()
            .ignoresSafeArea(.container, edges: .bottom)
            .onAppear(perform: Self.forceDeterministicConfiguration)
    }

    /// Pin the shared keyboard configuration to the exact engine setup the UI
    /// test's expected output was generated with (`funput dev run -m vni`).
    /// VNI + these flags equal `FunputConfiguration.default`, which is also
    /// what the extension falls back to when Full Access (and thus the App
    /// Group) is unavailable — so the test behaves the same either way.
    /// Theme and feedback fields are left untouched.
    static func forceDeterministicConfiguration() {
        let store = FunputConfigurationStore()
        var configuration = store.load()
        configuration.inputMethod = .vni
        configuration.language = .vietnamese
        configuration.toneStyle = .traditional
        configuration.spellCheck = false
        configuration.smartRestore = true
        configuration.eagerRestore = false
        configuration.autoCapitalize = false
        store.save(configuration)
    }
}

/// UITextView with every smart/auto feature disabled — the harness must show
/// exactly the characters the keyboard delivered, nothing more.
private struct HarnessTextView: UIViewRepresentable {
    func makeUIView(context: Context) -> UITextView {
        let view = UITextView()
        view.accessibilityIdentifier = "typingHarness.field"
        view.font = .preferredFont(forTextStyle: .title3)
        view.autocorrectionType = .no
        view.autocapitalizationType = .none
        view.spellCheckingType = .no
        view.smartQuotesType = .no
        view.smartDashesType = .no
        view.smartInsertDeleteType = .no
        view.keyboardType = .default
        return view
    }

    func updateUIView(_ view: UITextView, context: Context) {
        // Focus once the view lands in a window so the keyboard comes up
        // without the test having to tap first (tapping still works).
        DispatchQueue.main.async {
            if view.window != nil, !view.isFirstResponder {
                view.becomeFirstResponder()
            }
        }
    }
}

#Preview {
    TypingHarnessView()
}
