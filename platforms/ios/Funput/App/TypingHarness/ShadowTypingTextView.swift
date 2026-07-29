#if DEBUG
import SwiftUI
import UIKit

struct ShadowTypingTextView: UIViewRepresentable {
    @Binding var text: String
    @Binding var wantsFocus: Bool

    func makeCoordinator() -> Coordinator {
        Coordinator(text: $text)
    }

    func makeUIView(context: Context) -> UITextView {
        let view = UITextView()
        view.delegate = context.coordinator
        view.accessibilityIdentifier = "deviceShadowHarness.field"
        view.font = .preferredFont(forTextStyle: .title3)
        view.autocorrectionType = .no
        view.autocapitalizationType = .none
        view.spellCheckingType = .no
        view.smartQuotesType = .no
        view.smartDashesType = .no
        view.smartInsertDeleteType = .no
        view.keyboardType = .default
        view.layer.cornerRadius = 12
        view.layer.borderWidth = 1
        view.layer.borderColor = UIColor.separator.cgColor
        view.textContainerInset = .init(top: 12, left: 8, bottom: 12, right: 8)
        return view
    }

    func updateUIView(_ view: UITextView, context: Context) {
        if view.text != text { view.text = text }
        DispatchQueue.main.async {
            if wantsFocus, view.window != nil, !view.isFirstResponder {
                view.becomeFirstResponder()
            } else if !wantsFocus, view.isFirstResponder {
                view.resignFirstResponder()
            }
        }
    }

    final class Coordinator: NSObject, UITextViewDelegate {
        private var text: Binding<String>

        init(text: Binding<String>) {
            self.text = text
        }

        func textViewDidChange(_ textView: UITextView) {
            text.wrappedValue = textView.text
        }
    }
}
#endif
