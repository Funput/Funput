import KeyboardInput
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
        let stack = try RolloverTypingTestStack.make()
        var rng = TestRand(seed: 0x2026_0716)
        var pendingEchoes: [(afterKey: Int, context: String)] = []

        for (index, character) in RolloverTypingFixture.keys.enumerated() {
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

        if echoLag > 0 {
            // The final release also schedules a host callback. Drain every
            // callback after typing stops; otherwise the test can pass while
            // leaving the synchronizer in the exact poisoned state that breaks
            // the next real key press.
            pendingEchoes.append((RolloverTypingFixture.keys.count + echoLag, stack.document.text))
            for echo in pendingEchoes {
                stack.document.reportedContext = echo.context
                stack.coordinator.synchronizeDocument(stack.document, event: .textChanged)
            }
            stack.document.reportedContext = nil
        }

        XCTAssertEqual(
            stack.document.text,
            RolloverTypingFixture.expected,
            RolloverTypingFixture.diff(stack.document.text)
        )
    }
}
