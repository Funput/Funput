import XCTest

/// The typing harness with typo correction on, and one word typed into it with a
/// slipped key — shared by the correction UI tests.
///
/// The tap positions are the whole point. Correction only has something to work with
/// when the touch says a neighbouring key was close, so the slipped key is tapped
/// near the edge it drifted from rather than at its centre — which is exactly what a
/// thumb does at speed.
///
/// Prerequisites (simulator): run `Scripts/uitest-enable-keyboard.sh` once after
/// installing the app. On a device, enable the keyboard in Settings first.
@MainActor
struct TypoCorrectionHarness {
    /// `đường` in VNI is `d9u7o7ng2`; the finger lands on `h`, one key right of `g`.
    static let typed = "d9u7o7nh2"
    static let slipIndex = 7
    /// What those keys compose to when nothing repairs them.
    ///
    /// Not the raw keystrokes: the harness pins `eagerRestore = false` so its output
    /// stays deterministic, which leaves the composed form standing. The boundary does
    /// not restore it either — VNI spells with digits, so `keystrokes_intend_vietnamese`
    /// reads the word as deliberate.
    static let uncorrected = "đườnh"
    static let repaired = "đường"

    let app: XCUIApplication
    let field: XCUIElement

    init(allowsAutocorrect: Bool) throws {
        app = XCUIApplication()
        app.launchArguments += ["-uitest-typing-harness"]
        if allowsAutocorrect { app.launchArguments += ["-uitest-autocorrect"] }
        app.launch()

        field = app.textViews["typingHarness.field"]
        XCTAssertTrue(field.waitForExistence(timeout: 10), "typing harness field missing")
        field.tap()
        XCTAssertTrue(FunputKeyboardDriver.switchToFunputKeyboard(app), """
        Funput keyboard never appeared. Enable the extension first: \
        simulator → Scripts/uitest-enable-keyboard.sh, device → Settings › \
        General › Keyboard › Keyboards.
        """)
    }

    /// Close the app and drop the configuration override it was launched with.
    static func cleanup(_ app: XCUIApplication?) {
        app?.terminate()
        let cleanup = XCUIApplication()
        cleanup.launchArguments = ["-uitest-clear-configuration-override"]
        cleanup.launch()
        cleanup.terminate()
    }

    /// Tap every key at its centre, except the slipped one — tapped a sixth of the way
    /// in from the edge nearest the key it was meant for, so the keyboard reports that
    /// key as the near neighbour. Ends the word with a space unless told not to.
    func typeWithSlip(space: Bool = true) {
        let frames = FunputKeyboardDriver.resolveKeyFrames(app, for: Self.typed + " ")
        for (index, character) in Self.typed.enumerated() {
            guard let frame = frames[character] else { continue }
            let x = index == Self.slipIndex ? frame.minX + frame.width / 6 : frame.midX
            tap(x: x, y: frame.midY)
        }
        if space, let frame = frames[" "] { tap(x: frame.midX, y: frame.midY) }
    }

    /// Tap every key dead centre, the slipped one included. With nothing leaning
    /// towards `g`, the repair is one neighbour swap among others and the engine will
    /// not pick it on its own: Space leaves the word and parks the repair on the bar.
    func typeDeadCentre(space: Bool = true) {
        let frames = FunputKeyboardDriver.resolveKeyFrames(app, for: Self.typed + " ")
        for character in Self.typed {
            guard let frame = frames[character] else { continue }
            tap(x: frame.midX, y: frame.midY)
        }
        if space, let frame = frames[" "] { tap(x: frame.midX, y: frame.midY) }
    }

    func key(labeled label: String, exact: Bool = true) throws -> XCUICoordinate {
        let frame = try XCTUnwrap(
            FunputKeyboardDriver.keyFrame(app, labeled: label, exact: exact),
            "key \"\(label)\" not found"
        )
        return app.coordinate(withNormalizedOffset: .zero)
            .withOffset(CGVector(dx: frame.midX, dy: frame.midY))
    }

    /// A slot on the suggestion bar, found by the label VoiceOver reads for it. The
    /// slots carry the keyboard-key trait, so they surface as keys rather than
    /// buttons; matching on the label alone keeps the test out of that question.
    func barSlot(_ label: String) -> XCUIElement {
        app.descendants(matching: .any)
            .matching(NSPredicate(format: "label == %@", label))
            .firstMatch
    }

    /// What the bar is showing, for a failure message.
    func barLabels() -> [String] {
        let slots = app.descendants(matching: .any).matching(
            NSPredicate(format: "label BEGINSWITH %@ OR label BEGINSWITH %@ OR label BEGINSWITH %@ OR label BEGINSWITH %@",
                        "Gợi ý, ", "Giữ nguyên, ", "Sửa thành, ", "Hoàn tác, ")
        )
        return (0..<min(slots.count, 6)).map { slots.element(boundBy: $0).label }
    }

    func committedText() -> String {
        // The correction lands in the same transaction as the space, but the proxy
        // still reports asynchronously.
        Thread.sleep(forTimeInterval: 1)
        return (field.value as? String) ?? ""
    }

    private func tap(x: CGFloat, y: CGFloat) {
        app.coordinate(withNormalizedOffset: .zero)
            .withOffset(CGVector(dx: x, dy: y))
            .tap()
    }
}
