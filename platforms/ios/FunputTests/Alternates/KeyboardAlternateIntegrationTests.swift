import CoreGraphics
@_spi(Testing) import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

@MainActor
struct KeyboardAlternateIntegrationTests {
    @Test("Hold drag release commits only the selected Vietnamese alternate")
    func interaction() throws {
        let driver = KeyboardTouchTestDriver()
        let key = KeySpec(
            id: "a",
            label: "a",
            role: .character,
            shiftedLabel: "A",
            alternates: VietnameseKeyAlternates.values(for: "a")
        )
        let source = CGRect(x: 102, y: 198, width: 36, height: 44)
        driver.begin(
            token: 1,
            key: key,
            point: CGPoint(x: source.midX, y: source.midY),
            sourceFrame: source,
            containerBounds: CGRect(x: 0, y: 0, width: 390, height: 304)
        )
        driver.runNextRepeat()
        let destination = try #require(driver.alternateCenter(at: 1))
        driver.move(token: 1, key: nil, point: destination)
        driver.end(token: 1)

        #expect(driver.events.count == 2)
        guard case let .alternateSelected(value) = driver.events.last?.phase else {
            Issue.record("Expected alternate selection")
            return
        }
        #expect(value.text == "á")
        #expect(!driver.events.contains { $0.phase == .released })
    }

    @Test("Quick tap keeps the original release")
    func quickTap() {
        let driver = KeyboardTouchTestDriver()
        let key = KeySpec(
            id: "a",
            label: "a",
            role: .character,
            alternates: VietnameseKeyAlternates.values(for: "a")
        )
        let frame = CGRect(x: 102, y: 198, width: 36, height: 44)
        driver.begin(
            token: 1,
            key: key,
            point: CGPoint(x: frame.midX, y: frame.midY),
            sourceFrame: frame,
            containerBounds: CGRect(x: 0, y: 0, width: 390, height: 304)
        )
        driver.end(token: 1)
        #expect(driver.events.map(\.phase) == [.pressed, .released])
    }

    @Test("VoiceOver actions follow Shift and editor policy")
    func accessibility() throws {
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
        #expect(key.accessibilityCustomActions?.first?.name == "Chọn A")
        #expect(key.accessibilityCustomActions?.map(\.name).contains("Chọn Ậ") == true)
    }

    private func controls(in view: UIView) -> [UIControl] {
        view.subviews.flatMap { child in
            let own = child as? UIControl
            return (own.map { [$0] } ?? []) + controls(in: child)
        }
    }
}
