#if DEBUG
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

    /// Pin the keyboard to the exact engine setup used to generate the test's
    /// expected output, without overwriting the user's persisted settings.
    static func forceDeterministicConfiguration() {
        var configuration = FunputConfiguration.default
        configuration.inputMethod = .vni
        configuration.language = .vietnamese
        configuration.toneStyle = .traditional
        configuration.spellCheck = false
        configuration.smartRestore = true
        configuration.eagerRestore = false
        configuration.autoCapitalize = false
        configuration.showsNumberRow = false
        configuration.showsGlobeKey = false
        FunputUITestConfigurationOverrideStore().save(
            configuration,
            expiresAt: Date().addingTimeInterval(10 * 60)
        )
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
#endif
