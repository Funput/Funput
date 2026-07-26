#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

@MainActor
struct KeyboardAlternateAccessibilityTests {
    @Test("Letter keys expose every alternate as a VoiceOver custom action")
    func actions() throws {
        var presentation = KeyboardPresentation(
            layout: StandardKeyboardLayouts.letters(.telex)
        )
        presentation.shiftState = .uppercase
        let surface = KeyboardSurfaceView(presentation: presentation)
        surface.frame = CGRect(x: 0, y: 0, width: 390, height: 304)
        surface.layoutIfNeeded()

        let key = try #require(controls(in: surface).first {
            $0.accessibilityLabel == "a"
        })
        let names = key.accessibilityCustomActions?.map(\.name) ?? []
        #expect(names.first == "Chọn A")
        #expect(names.contains("Chọn Ắ"))
        #expect(names.contains("Chọn Ậ"))
        #expect(names.count == 18)
    }

    @Test("Editors without alternates expose no accent actions")
    func excludedEditor() throws {
        let layout = EditorKeyboardLayouts.resolve(.telex, editorMode: .email)
        let surface = KeyboardSurfaceView(presentation: KeyboardPresentation(layout: layout))
        surface.frame = CGRect(x: 0, y: 0, width: 390, height: 304)
        surface.layoutIfNeeded()
        let key = try #require(controls(in: surface).first {
            $0.accessibilityLabel == "a"
        })
        #expect(key.accessibilityCustomActions == nil)
    }

    private func controls(in view: UIView) -> [UIControl] {
        view.subviews.flatMap { child in
            let own = child as? UIControl
            return (own.map { [$0] } ?? []) + controls(in: child)
        }
    }
}
#endif
