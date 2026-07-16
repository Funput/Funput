import XCTest

/// End-to-end Vietnamese typing through the real Funput keyboard extension,
/// one sequential tap per key. The single-touch baseline: if this fails, the
/// problem is not rollover-related (see RolloverTypingUITests for the
/// overlapping-touch variant that models real fast typing).
///
/// Launches the app in its typing-harness mode (`-uitest-typing-harness`),
/// switches the system keyboard to Funput, then taps out the shared VNI
/// paragraph and asserts the committed text matches the engine's reference
/// output exactly (see VNIParagraph).
///
/// Prerequisites (simulator): run `Scripts/uitest-enable-keyboard.sh` once
/// after installing the app so the extension is in the enabled-keyboards list.
/// On a real device, enable the keyboard in Settings first.
final class VietnameseTypingUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    @MainActor
    func testVNIParagraphAtHumanSpeedCommitsExactly() throws {
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
        let taps = FunputKeyboardDriver.resolveKeyCoordinates(app, for: VNIParagraph.keys)

        // Human pace: XCUITest tap synthesis already costs ~100–200 ms; the
        // extra jittered delay lands the run at roughly 3–5 keys/second.
        // Deterministic RNG so a failing run can be replayed exactly.
        var rng = SplitMix64(seed: 0xF0F0_2026)
        let baseDelayMs = UInt32(ProcessInfo.processInfo.environment["FUNPUT_TYPING_DELAY_MS"] ?? "") ?? 0
        for character in VNIParagraph.keys {
            taps[character]!.tap()
            usleep((baseDelayMs + rng.next(upTo: 120)) * 1000)
        }

        // Let the last composition settle before reading the field back.
        Thread.sleep(forTimeInterval: 1)
        let actual = (field.value as? String) ?? ""
        attachText(actual, name: "actual")
        attachText(VNIParagraph.expected, name: "expected")
        XCTAssertEqual(actual, VNIParagraph.expected, VNIParagraph.diffMessage(actual: actual))
    }
}
