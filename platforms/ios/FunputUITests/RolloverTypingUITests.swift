import XCTest

/// Fast two-thumb typing against the real Funput keyboard extension, with
/// true multi-touch rollover: the next key regularly goes DOWN before the
/// previous key comes UP, exactly like a fast human typist. This is the
/// pattern plain XCUIElement.tap() (strictly sequential) can never produce
/// and the one behind "lost characters while typing fast" reports.
///
/// Presses and releases both happen in text order, so with commit-on-release
/// the committed text must still equal the engine reference exactly — any
/// deviation is a real input bug, not typist error.
///
/// Same prerequisites as VietnameseTypingUITests. Tune speed with the
/// FUNPUT_TYPING_TEMPO env var (default 1.0; 2.0 = twice as slow).
final class RolloverTypingUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    @MainActor
    func testVNIParagraphWithTwoThumbRolloverCommitsExactly() throws {
        #if targetEnvironment(simulator)
        throw XCTSkip("""
        The simulator's synthesis daemon corrupts overlapping pointer \
        playback (phantom up/down/cancel transitions) — run this test on a \
        real device. On the simulator, rollover is covered in-process by \
        FunputTests/RolloverTouchPipelineTests.
        """)
        #else
        try runParagraph(allowOverlap: true)
        #endif
    }

    /// Control experiment: identical synthesizer, timings, and jitter, but
    /// each touch is forced to lift before the next goes down. Green here
    /// while the rollover variant fails isolates overlapping touches as the
    /// trigger and rules out the synthesis pipeline itself.
    @MainActor
    func testVNIParagraphSameSynthesizerWithoutOverlapCommitsExactly() throws {
        try runParagraph(allowOverlap: false)
    }

    @MainActor
    private func runParagraph(allowOverlap: Bool) throws {
        let app = XCUIApplication()
        app.launchArguments += ["-uitest-typing-harness"]
        app.launch()

        let field = app.textViews["typingHarness.field"]
        XCTAssertTrue(field.waitForExistence(timeout: 10), "typing harness field missing")
        field.tap()

        XCTAssertTrue(FunputKeyboardDriver.switchToFunputKeyboard(app), """
        Funput keyboard never appeared. Enable the extension first: \
        simulator → Scripts/uitest-enable-keyboard.sh, device → Settings › \
        General › Keyboard › Keyboards.
        """)
        let frames = FunputKeyboardDriver.resolveKeyFrames(app, for: VNIParagraph.keys)

        let tempo = Double(ProcessInfo.processInfo.environment["FUNPUT_TYPING_TEMPO"] ?? "") ?? 1.0
        var rng = SplitMix64(seed: 0x2026_0716)
        // One synthesized batch per word keeps at most a handful of pointer
        // paths per record; the pause between batches is the natural word gap.
        let words = VNIParagraph.keys.split(separator: " ", omittingEmptySubsequences: false)
        for (index, word) in words.enumerated() {
            let chunk = index < words.count - 1 ? String(word) + " " : String(word)
            let strokes = planStrokes(
                for: chunk, frames: frames, tempo: tempo, allowOverlap: allowOverlap, rng: &rng
            )
            try MultiTouchSynthesizer.perform(strokes, in: self)
            usleep(UInt32(rng.next(in: 120_000...240_000) * tempo)) // inter-word gap
        }

        Thread.sleep(forTimeInterval: 1)
        let actual = (field.value as? String) ?? ""
        attachText(actual, name: "actual")
        attachText(VNIParagraph.expected, name: "expected")
        XCTAssertEqual(actual, VNIParagraph.expected, VNIParagraph.diffMessage(actual: actual))
    }

    /// Builds one word's touch timeline. Down→down gaps of 110–190 ms with
    /// holds of 100–170 ms make holds frequently outlast gaps, so consecutive
    /// keys overlap (rollover) without ever being scripted explicitly.
    /// Invariants that keep the expected output well-defined:
    ///  - releases stay in press order (a clean roll, what the engine's
    ///    commit-on-release path must handle losslessly)
    ///  - at most two fingers are down at once (two thumbs)
    ///  - a repeated key waits for its own previous release (one physical key
    ///    cannot be pressed twice concurrently)
    /// Events delivered within one ~17 ms digitizer frame can reach the app
    /// in either order; keeping ordered pairs of downs and ups two frames
    /// apart guarantees the frame boundary can never merge or reorder them —
    /// a same-frame swap would scramble text without any app bug. Real rolls
    /// have ≥30 ms between releases anyway.
    private static let minEventSeparation: TimeInterval = 0.034

    private func planStrokes(
        for chunk: String,
        frames: [Character: CGRect],
        tempo: Double,
        allowOverlap: Bool,
        rng: inout SplitMix64
    ) -> [TouchStroke] {
        var strokes: [TouchStroke] = []
        var cursor: TimeInterval = 0.06 // settle before the first touch
        var previousCharacter: Character?
        for character in chunk {
            let frame = frames[character]!
            var down = cursor
            if !allowOverlap || character == previousCharacter, let last = strokes.last {
                down = max(down, last.upOffset + Self.minEventSeparation)
            }
            if strokes.count >= 2 {
                down = max(down, strokes[strokes.count - 2].upOffset + Self.minEventSeparation)
            }
            var up = down + rng.next(in: 0.100...0.170) * tempo
            if let last = strokes.last {
                up = max(up, last.upOffset + Self.minEventSeparation)
            }
            strokes.append(TouchStroke(
                point: jitteredPoint(in: frame, rng: &rng),
                downOffset: down,
                upOffset: up
            ))
            cursor = down + rng.next(in: 0.110...0.190) * tempo
            previousCharacter = character
        }
        return strokes
    }

    /// Humans do not hit keycap centers: scatter within the middle ~70% of
    /// the key so hit-testing near edges is exercised without ever missing.
    private func jitteredPoint(in frame: CGRect, rng: inout SplitMix64) -> CGPoint {
        CGPoint(
            x: frame.midX + frame.width * rng.next(in: -0.18...0.18),
            y: frame.midY + frame.height * rng.next(in: -0.18...0.18)
        )
    }
}
