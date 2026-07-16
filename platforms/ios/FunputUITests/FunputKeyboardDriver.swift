import XCTest

/// Locates and taps keys on the Funput keyboard extension for typing UI tests
/// (see `VietnameseTypingUITests`).
enum FunputKeyboardDriver {
    /// VNI digit keys carry Vietnamese accessibility labels, not "1"..."9"
    /// (see TopNumberRowFactory).
    static let vniDigitLabels: [Character: String] = [
        "1": "Dấu sắc", "2": "Dấu huyền", "3": "Dấu hỏi", "4": "Dấu ngã",
        "5": "Dấu nặng", "6": "Dấu mũ", "7": "Dấu móc", "8": "Dấu trăng",
        "9": "Chữ đ", "0": "Xóa dấu",
    ]

    /// The Funput layer is identified by its VNI tone key ("Dấu sắc" — no
    /// system keyboard has one); the system keyboard's globe key is tapped
    /// until it shows up. The extension's toolbar buttons are NOT exposed to
    /// XCUITest, so only real keys can serve as the sentinel.
    /// Returns false when the Funput keyboard never appeared.
    static func switchToFunputKeyboard(_ app: XCUIApplication) -> Bool {
        let sentinel = app.keys["Dấu sắc"]
        if sentinel.waitForExistence(timeout: 5) { return true }

        for _ in 0..<4 {
            let globe = app.buttons
                .matching(NSPredicate(format: "label ==[c] 'next keyboard' OR label == 'Bàn phím tiếp theo'"))
                .firstMatch
            guard globe.waitForExistence(timeout: 3) else { break }
            globe.tap()
            if sentinel.waitForExistence(timeout: 3) { return true }
        }
        return false
    }

    /// Resolve every unique character in `keys` to a screen coordinate once,
    /// up front. Tapping cached coordinates keeps the per-keystroke cost flat
    /// (no accessibility re-query per tap) and avoids matching the typed text
    /// itself, which after a while contains the same letters as the keycaps.
    static func resolveKeyCoordinates(
        _ app: XCUIApplication, for keys: String
    ) -> [Character: XCUICoordinate] {
        var taps: [Character: XCUICoordinate] = [:]
        for character in Set(keys) {
            let label: String
            var exactLabel = true
            if character == " " {
                label = "Dấu cách"
                exactLabel = false // full label carries the swipe hint
            } else if let digitLabel = vniDigitLabels[character] {
                label = digitLabel
            } else {
                label = String(character)
            }
            let element = keyElement(app, labeled: label, exact: exactLabel)
            XCTAssertTrue(
                element.waitForExistence(timeout: 3),
                "key '\(character)' (label \"\(label)\") not found on the Funput keyboard"
            )
            // Absolute screen coordinate: element-relative XCUICoordinates
            // re-resolve their element on every tap, which is slow and can go
            // stale mid-run; the keycap frames never move while typing.
            let frame = element.frame
            taps[character] = app
                .coordinate(withNormalizedOffset: .zero)
                .withOffset(CGVector(dx: frame.midX, dy: frame.midY))
        }
        return taps
    }

    /// A keycap in the keyboard area. Prefers real `.key` elements; falls back
    /// to any labeled element in the lower half of the screen so the query can
    /// never land on the harness text content.
    private static func keyElement(
        _ app: XCUIApplication, labeled label: String, exact: Bool
    ) -> XCUIElement {
        let format = exact ? "label == %@" : "label BEGINSWITH %@"
        let predicate = NSPredicate(format: format, label)
        let key = app.keys.matching(predicate).firstMatch
        if key.exists { return key }

        let screenMidY = app.frame.midY
        let candidates = app.descendants(matching: .any).matching(predicate)
        for index in 0..<min(candidates.count, 8) {
            let candidate = candidates.element(boundBy: index)
            if candidate.frame.minY > screenMidY { return candidate }
        }
        return key // report the missing .key element in the assertion
    }
}

/// Tiny deterministic RNG (SplitMix64) so typing jitter is reproducible.
struct SplitMix64 {
    private var state: UInt64

    init(seed: UInt64) { state = seed }

    mutating func next(upTo bound: UInt32) -> UInt32 {
        state &+= 0x9E37_79B9_7F4A_7C15
        var z = state
        z = (z ^ (z >> 30)) &* 0xBF58_476D_1CE4_E5B9
        z = (z ^ (z >> 27)) &* 0x94D0_49BB_1331_11EB
        return UInt32((z ^ (z >> 31)) % UInt64(bound))
    }
}
