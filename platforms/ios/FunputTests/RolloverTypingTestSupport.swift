import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import XCTest

enum RolloverTypingFixture {
    static let keys =
        "ho6m nay tro7i2 trong xanh minh2 d9i dao5 quanh ho62 nho3 ro6i2 "
        + "ghe1 quan1 ca2 phe6 goi5 mo6t5 ly su7a4 d9a1 ngo6i2 nga8m1 dong2 "
        + "ngu7o7i2 qua lai5"
    static let expected =
        "hôm nay trời trong xanh mình đi dạo quanh hồ nhỏ rồi ghé quán cà "
        + "phê gọi một ly sữa đá ngồi ngắm dòng người qua lại"
    static let vniDigitLabels: [Character: String] = [
        "1": "Dấu sắc", "2": "Dấu huyền", "3": "Dấu hỏi", "4": "Dấu ngã",
        "5": "Dấu nặng", "6": "Dấu mũ", "7": "Dấu móc", "8": "Dấu trăng",
        "9": "Chữ đ", "0": "Xóa dấu",
    ]

    static func diff(_ actual: String) -> String {
        let common = zip(actual, expected).prefix { $0.0 == $0.1 }
        return "committed \(actual.count)/\(expected.count) chars — "
            + "first divergence after: \"\(String(common.map(\.0)))\""
    }
}

@MainActor
final class RolloverTypingTestStack {
    var interaction: [Character: UIControl] = [:]
    var lastInteraction: UIControl?
    let document = ScriptedDocument()
    let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
    var surface: KeyboardSurfaceView!
    var window: UIWindow!

    static func make() throws -> RolloverTypingTestStack {
        let stack = RolloverTypingTestStack()
        stack.window = UIWindow(frame: CGRect(x: 0, y: 0, width: 402, height: 874))
        stack.window.rootViewController = UIViewController()
        stack.window.makeKeyAndVisible()

        var presentation = KeyboardPresentation()
        presentation.layout = StandardKeyboardLayouts.letters(.vni)
        presentation.isHapticFeedbackEnabled = false
        presentation.isKeySoundEnabled = false
        stack.surface = KeyboardSurfaceView(presentation: presentation)
        XCTAssertTrue(stack.surface.setTouchPipelineMode(.legacy))
        stack.surface.frame = CGRect(x: 0, y: 574, width: 402, height: 300)
        stack.window.rootViewController?.view.addSubview(stack.surface)
        stack.window.layoutIfNeeded()
        stack.surface.layoutIfNeeded()
        stack.routeInputEvents()
        try stack.resolveControls()
        return stack
    }

    private func routeInputEvents() {
        surface.onKeyEvent = { [weak self] event in
            guard let self else { return }
            switch event.phase {
            case .released, .repeated:
                coordinator.handle(event.key, document: document)
            case .pressed, .cancelled, .swiped, .alternateSelected:
                break
            }
        }
    }

    private func resolveControls() throws {
        let keyControls = accessibleControls(in: surface)
        for character in Set(RolloverTypingFixture.keys) {
            let key = try XCTUnwrap(keyControls.first { control in
                guard let label = control.accessibilityLabel else { return false }
                if character == " " { return label.hasPrefix("Dấu cách") }
                return label == RolloverTypingFixture.vniDigitLabels[character]
                    ?? String(character)
            }, "key '\(character)' not found")
            interaction[character] = try XCTUnwrap(
                key.subviews.compactMap { $0 as? UIControl }.first,
                "interaction control missing for '\(character)'"
            )
        }
    }
}
