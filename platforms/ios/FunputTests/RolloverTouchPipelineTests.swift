import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import XCTest

/// Types a full VNI paragraph through the real surface + coordinator stack,
/// isolating the two things fast human typing does that sequential tests
/// never exercise:
///
/// 1. **Rollover** — the next key's touch-down lands before the previous
///    key's touch-up (interleaved control events, commit on release).
/// 2. **Stale host echoes** — `textDidChange` arriving late, reporting the
///    context from one or two mutations ago, exactly like the real
///    `documentContextBeforeInput` does during fast typing.
///
/// Presses and releases stay in text order, so the committed text must equal
/// the engine reference exactly; any drop, duplicate, or literal tone digit
/// is an input-path bug.
@MainActor
final class RolloverTouchPipelineTests: XCTestCase {
    /// Same fixture as FunputUITests/VNIParagraph.swift:
    ///   cargo run -p funput-cli -- dev run -m vni "<keys>"
    private static let keys =
        "ho6m nay tro7i2 trong xanh minh2 d9i dao5 quanh ho62 nho3 ro6i2 "
        + "ghe1 quan1 ca2 phe6 goi5 mo6t5 ly su7a4 d9a1 ngo6i2 nga8m1 dong2 "
        + "ngu7o7i2 qua lai5"
    private static let expected =
        "hôm nay trời trong xanh mình đi dạo quanh hồ nhỏ rồi ghé quán cà "
        + "phê gọi một ly sữa đá ngồi ngắm dòng người qua lại"

    private static let vniDigitLabels: [Character: String] = [
        "1": "Dấu sắc", "2": "Dấu huyền", "3": "Dấu hỏi", "4": "Dấu ngã",
        "5": "Dấu nặng", "6": "Dấu mũ", "7": "Dấu móc", "8": "Dấu trăng",
        "9": "Chữ đ", "0": "Xóa dấu",
    ]

    func testRolloverOrderingCommitsExactly() throws {
        try runParagraph(rollover: true, echoLag: 0)
    }

    func testStaleHostEchoesCommitExactly() throws {
        try runParagraph(rollover: false, echoLag: 2)
    }

    func testRolloverWithStaleHostEchoesCommitsExactly() throws {
        try runParagraph(rollover: true, echoLag: 2)
    }

    /// `echoLag` > 0 delivers a `.textChanged` echo after every key, but only
    /// after `echoLag` further keys have typed, reporting the context as it
    /// was when the echoed key committed — the host's real callback pattern.
    private func runParagraph(rollover: Bool, echoLag: Int) throws {
        let stack = try makeStack()
        var rng = TestRand(seed: 0x2026_0716)
        var pendingEchoes: [(afterKey: Int, context: String)] = []

        for (index, character) in Self.keys.enumerated() {
            let controls = try XCTUnwrap(stack.interaction[character])
            if rollover, let previous = stack.lastInteraction {
                // Next key down BEFORE previous key up, then ordered releases.
                controls.sendActions(for: .touchDown)
                previous.sendActions(for: .touchUpInside)
            } else {
                stack.lastInteraction?.sendActions(for: .touchUpInside)
                controls.sendActions(for: .touchDown)
            }
            stack.lastInteraction = controls

            if echoLag > 0 {
                pendingEchoes.append((index + echoLag, stack.document.text))
                while let echo = pendingEchoes.first, echo.afterKey <= index {
                    pendingEchoes.removeFirst()
                    stack.document.reportedContext = echo.context
                    stack.coordinator.synchronizeDocument(stack.document, event: .textChanged)
                    stack.document.reportedContext = nil
                }
            }
            _ = rng.next(upTo: 2) // keep the RNG in the shared cadence
        }
        stack.lastInteraction?.sendActions(for: .touchUpInside)

        XCTAssertEqual(stack.document.text, Self.expected, diff(stack.document.text))
    }

    // MARK: - Stack

    @MainActor
    private final class Stack {
        var interaction: [Character: UIControl] = [:]
        var lastInteraction: UIControl?
        let document = ScriptedDocument()
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        var surface: KeyboardSurfaceView!
        var window: UIWindow!
    }

    /// Real KeyboardSurfaceView wired to a real coordinator, routed exactly
    /// like KeyboardViewController.handleKeyEvent (releases + repeats only).
    private func makeStack() throws -> Stack {
        let stack = Stack()
        stack.window = UIWindow(frame: CGRect(x: 0, y: 0, width: 402, height: 874))
        stack.window.rootViewController = UIViewController()
        stack.window.makeKeyAndVisible()

        var presentation = KeyboardPresentation()
        presentation.layout = StandardKeyboardLayouts.letters(.vni)
        presentation.isHapticFeedbackEnabled = false
        presentation.isKeySoundEnabled = false
        stack.surface = KeyboardSurfaceView(presentation: presentation)
        stack.surface.frame = CGRect(x: 0, y: 574, width: 402, height: 300)
        stack.window.rootViewController?.view.addSubview(stack.surface)
        stack.window.layoutIfNeeded()
        stack.surface.layoutIfNeeded()

        stack.surface.onKeyEvent = { [weak stack] event in
            guard let stack else { return }
            switch event.phase {
            case .released, .repeated:
                stack.coordinator.handle(event.key, document: stack.document)
            case .pressed, .cancelled, .swiped:
                break
            }
        }

        let keyControls = accessibleControls(in: stack.surface)
        for character in Set(Self.keys) {
            let key = try XCTUnwrap(
                keyControls.first { control in
                    guard let label = control.accessibilityLabel else { return false }
                    if character == " " { return label.hasPrefix("Dấu cách") }
                    return label == Self.vniDigitLabels[character] ?? String(character)
                },
                "key '\(character)' not found"
            )
            // The touch-tracking child control inside KeyboardKeyControl.
            stack.interaction[character] = try XCTUnwrap(
                key.subviews.compactMap { $0 as? UIControl }.first,
                "interaction control missing for '\(character)'"
            )
        }
        return stack
    }

    private func diff(_ actual: String) -> String {
        let common = zip(actual, Self.expected).prefix { $0.0 == $0.1 }
        return "committed \(actual.count)/\(Self.expected.count) chars — "
            + "first divergence after: \"\(String(common.map(\.0)))\""
    }
}
