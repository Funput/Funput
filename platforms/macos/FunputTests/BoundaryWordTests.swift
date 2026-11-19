import XCTest
@testable import Funput

/// The word a boundary key commits in Vietnamese mode (`FunputComposer.endWord`).
@MainActor
final class BoundaryWordTests: XCTestCase {
    private let longExpansion =
        "Cộng hoà Xã hội Chủ nghĩa Việt Nam, Độc lập, Tự do, Hạnh phúc. Số 1 Đường Hùng Vương, Hà Nội"

    func testAnExpansionLongerThanTheInlineResultComesThroughWhole() {
        XCTAssertGreaterThan(longExpansion.unicodeScalars.count, Int(CHARS_CAP))
        for boundary: Unicode.Scalar in [" ", ".", "\r", "\t"] {
            let composer = FunputComposer()
            composer.addShortcut(trigger: "chxh", expansion: longExpansion)
            for key in "chxh".unicodeScalars { composer.process(key) }
            XCTAssertEqual(composer.endWord(with: boundary), longExpansion)
            XCTAssertEqual(composer.buffer(), "")
        }
    }

    func testAPlainSyllableIsLeftAsComposed() {
        let composer = FunputComposer()
        composer.apply(.init(inputMethod: .telex))
        for key in "vieetj".unicodeScalars { composer.process(key) }
        XCTAssertNil(composer.endWord(with: " "))
    }

    func testANumpadDigitEndsATriggerInsteadOfTakingATone() {
        let composer = FunputComposer()
        composer.apply(.init(inputMethod: .vni))
        composer.addShortcut(trigger: "vn", expansion: "Việt Nam")
        for key in "vn".unicodeScalars { composer.process(key) }
        XCTAssertEqual(composer.endWord(with: "5", source: .numpad), "Việt Nam")
    }
}
