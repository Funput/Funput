import XCTest

/// End-to-end Vietnamese typing through the real Funput keyboard extension.
///
/// Launches the app in its typing-harness mode (`-uitest-typing-harness`),
/// switches the system keyboard to Funput, then taps out a full VNI paragraph
/// key by key at a human pace and asserts the committed text matches the
/// engine's reference output exactly. Reproduces the "lost characters while
/// typing" report: any dropped tap or missed backspace shows up as a diff.
///
/// Prerequisites (simulator): run `Scripts/uitest-enable-keyboard.sh` once
/// after installing the app so the extension is in the enabled-keyboards list.
/// On a real device, enable the keyboard in Settings first.
final class VietnameseTypingUITests: XCTestCase {
    /// VNI key sequence for the paragraph. Reference output generated with the
    /// shared Rust engine, which is the ground truth for every platform:
    ///   cargo run -p funput-cli -- dev run -m vni "<keys>"
    private static let vniKeys =
        "ho6m nay tro7i2 trong xanh minh2 d9i dao5 quanh ho62 nho3 ro6i2 "
        + "ghe1 quan1 ca2 phe6 goi5 mo6t5 ly su7a4 d9a1 ngo6i2 nga8m1 dong2 "
        + "ngu7o7i2 qua lai5"

    private static let expected =
        "hôm nay trời trong xanh mình đi dạo quanh hồ nhỏ rồi ghé quán cà "
        + "phê gọi một ly sữa đá ngồi ngắm dòng người qua lại"

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
        let taps = FunputKeyboardDriver.resolveKeyCoordinates(app, for: Self.vniKeys)

        // Human pace: XCUITest tap synthesis already costs ~100–200 ms; the
        // extra jittered delay lands the run at roughly 3–5 keys/second.
        // Deterministic RNG so a failing run can be replayed exactly.
        var rng = SplitMix64(seed: 0xF0F0_2026)
        let baseDelayMs = UInt32(ProcessInfo.processInfo.environment["FUNPUT_TYPING_DELAY_MS"] ?? "") ?? 60
        for character in Self.vniKeys {
            taps[character]!.tap()
            usleep((baseDelayMs + rng.next(upTo: 120)) * 1000)
        }

        // Let the last composition settle before reading the field back.
        Thread.sleep(forTimeInterval: 1)
        let actual = (field.value as? String) ?? ""
        attach(actual, name: "actual")
        attach(Self.expected, name: "expected")
        XCTAssertEqual(actual, Self.expected, diffMessage(actual: actual))
    }

    /// Pinpoints the failure instead of dumping two long strings: character
    /// counts (drops show up as a shorter actual) plus the first diverging
    /// word so the lost keystroke is obvious.
    private func diffMessage(actual: String) -> String {
        let expectedWords = Self.expected.split(separator: " ", omittingEmptySubsequences: false)
        let actualWords = actual.split(separator: " ", omittingEmptySubsequences: false)
        var message = "committed text differs from engine reference — "
            + "\(actual.count)/\(Self.expected.count) chars, "
            + "\(actualWords.count)/\(expectedWords.count) words"
        for (index, expectedWord) in expectedWords.enumerated() {
            let actualWord = index < actualWords.count ? actualWords[index] : ""
            if actualWord != expectedWord {
                message += "; first mismatch at word \(index + 1): "
                    + "expected \"\(expectedWord)\", got \"\(actualWord)\""
                break
            }
        }
        return message
    }

    private func attach(_ string: String, name: String) {
        let attachment = XCTAttachment(string: string)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
