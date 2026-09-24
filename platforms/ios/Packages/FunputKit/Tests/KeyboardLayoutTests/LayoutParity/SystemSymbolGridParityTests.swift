import CoreGraphics
import KeyboardLayout
import Testing

/// The system symbol pages with system sizing, checked against the iOS 27 stock keyboard.
struct SystemSymbolGridParityTests {
    /// Key edges in points of the "123" page's third row on a 393pt phone, measured from a
    /// 3x screenshot of the stock Vietnamese keyboard: `#+= . , ? ! ' delete`.
    static let stock393: [ClosedRange<CGFloat>] = [
        6.7...50.7, 64.3...112.3, 118.3...166.0, 172.3...220.3,
        226.7...274.3, 280.7...328.3, 342.3...386.3,
    ]

    @Test("The punctuation row sits where Apple puts it")
    func matchesStock() {
        let row = resolve(SystemSymbolKeyboardLayouts.primary(.telex), width: 393)[2]
        #expect(row.count == Self.stock393.count)
        for (key, expected) in zip(row, Self.stock393) {
            #expect(abs(key.frame.minX - expected.lowerBound) <= 1.5, "\(key.spec.label)")
            #expect(abs(key.frame.maxX - expected.upperBound) <= 1.5, "\(key.spec.label)")
        }
    }

    @Test("Both pages line up with the letters page", arguments: [390.0, 393.0, 402.0, 440.0])
    func linesUpWithLetters(width: CGFloat) throws {
        let letters = resolve(SystemKeyboardLayouts.letters(.telex, showsNumberRow: false), width: width)
        let shift = try #require(letters[2].first { $0.spec.role == .shift }).frame
        let delete = try #require(letters[2].first { $0.spec.role == .backspace }).frame
        let z = try #require(letters[2].first { $0.spec.label == "z" }).frame
        let m = try #require(letters[2].first { $0.spec.label == "m" }).frame

        let pages = [
            SystemSymbolKeyboardLayouts.primary(.telex),
            SystemSymbolKeyboardLayouts.secondary(.telex),
        ]
        for page in pages {
            let row = resolve(page, width: width)[2]
            let pageSwitch = try #require(row.first).frame
            let backspace = try #require(row.last).frame
            let punctuation = row.dropFirst().dropLast().map(\.frame)

            #expect(pageSwitch.minX == shift.minX && pageSwitch.width == shift.width)
            #expect(backspace.minX == delete.minX && backspace.width == delete.width)
            #expect(abs((punctuation.first?.minX ?? 0) - z.minX) <= 0.5, "starts under z")
            #expect(abs((punctuation.last?.maxX ?? 0) - m.maxX) <= 0.5, "ends under m")
            #expect(Set(punctuation.map(\.width)).count == 1, "equal punctuation keys")
        }
    }

    private func resolve(_ layout: KeyboardLayout, width: CGFloat) -> [[ResolvedKey]] {
        KeyboardGeometry.resolve(
            layout: layout,
            size: CGSize(width: width, height: 255),
            sizing: .system
        ).rows
    }
}
