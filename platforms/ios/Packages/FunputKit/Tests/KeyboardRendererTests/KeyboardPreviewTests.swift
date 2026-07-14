#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

@MainActor
struct KeyboardPreviewTests {
    @Test("Preview renders shifted printable label and rejects controls")
    func previewContent() {
        let preview = KeyboardKeyPreviewView()
        var presentation = KeyboardPresentation()
        presentation.shiftState = .uppercase
        let source = CGRect(x: 20, y: 100, width: 36, height: 44)
        let bounds = CGRect(x: 0, y: 0, width: 390, height: 304)

        preview.show(
            key: key("a"),
            sourceFrame: source,
            presentation: presentation,
            traits: UITraitCollection(userInterfaceStyle: .light),
            containerBounds: bounds
        )

        #expect(!preview.isHidden)
        #expect(labels(in: preview).contains("A"))
        #expect(bounds.contains(preview.frame))

        preview.show(
            key: KeySpec(id: "shift", label: "", role: .shift),
            sourceFrame: source,
            presentation: presentation,
            traits: UITraitCollection(),
            containerBounds: bounds
        )
        #expect(preview.isHidden)
    }

    @Test("Secure presentation suppresses key previews")
    func securePreview() {
        var previewCount = 0
        let controller = KeyboardSurfaceInteractionController(
            onEvent: { _ in },
            onPreview: { key, _ in
                if key != nil { previewCount += 1 }
            }
        )
        var presentation = KeyboardPresentation()
        presentation.isHapticFeedbackEnabled = false
        presentation.showsKeyPreviews = false

        controller.handle(
            KeyboardKeyEvent(key: key("a"), phase: .pressed),
            sourceFrame: .zero,
            presentation: presentation
        )

        #expect(previewCount == 0)
    }

    private func key(_ label: String) -> KeySpec {
        KeySpec(
            id: "key-\(label)",
            label: label,
            role: .character,
            shiftedLabel: label.uppercased()
        )
    }

    private func labels(in view: UIView) -> [String] {
        view.subviews.flatMap { child in
            let own = (child as? UILabel)?.text.map { [$0] } ?? []
            return own + labels(in: child)
        }
    }
}
#endif
