import AppKit
import XCTest
@testable import Funput

final class EnglishCompositionTests: XCTestCase {
    private func composer() -> FunputComposer {
        let composer = FunputComposer()
        composer.apply(.init(enabled: false, autoCapitalizeEnabled: true))
        composer.addShortcut(trigger: "vn", expansion: "Việt Nam")
        return composer
    }

    func testExpansionOwnsPreeditWithoutAnyDocumentQueries() {
        let composer = composer()
        XCTAssertEqual(EnglishComposition.process("v", composer: composer),
                       .init(text: "v", marked: true, handled: true))
        XCTAssertEqual(EnglishComposition.process("n", composer: composer),
                       .init(text: "vn", marked: true, handled: true))
        XCTAssertEqual(EnglishComposition.process(" ", composer: composer),
                       .init(text: "Việt Nam ", marked: false, handled: true))
        XCTAssertEqual(composer.buffer(), "")
    }

    func testNonShortcutCommitsLiteralKeysAndBoundaryOnce() {
        let composer = composer()
        for scalar in "address".unicodeScalars {
            _ = EnglishComposition.process(scalar, composer: composer)
        }
        XCTAssertEqual(composer.buffer(), "address")
        XCTAssertEqual(EnglishComposition.process(".", composer: composer),
                       .init(text: "address.", marked: false, handled: true))
        XCTAssertEqual(EnglishComposition.process(" ", composer: composer),
                       .init(text: " ", marked: false, handled: true))
    }

    func testEnterAndTabCommitButKeepNativeAction() {
        for scalar: Unicode.Scalar in ["\r", "\n", "\t"] {
            let composer = composer()
            for key in "vn".unicodeScalars { _ = EnglishComposition.process(key, composer: composer) }
            XCTAssertEqual(EnglishComposition.process(scalar, composer: composer),
                           .init(text: "Việt Nam", marked: false, handled: false))
        }
    }

    func testBackspaceCorrectsTriggerAndDoesNotReopenCommittedWord() {
        let composer = composer()
        for key in "vnx".unicodeScalars { _ = EnglishComposition.process(key, composer: composer) }
        XCTAssertEqual(EnglishComposition.backspace(composer: composer),
                       .init(text: "vn", marked: true, handled: true))
        _ = EnglishComposition.process(" ", composer: composer)
        XCTAssertNil(EnglishComposition.backspace(composer: composer))
    }

    func testBackspaceDeletesWholeGraphemeAndCanEmptyPreedit() {
        let composer = composer()
        for key in "e\u{301}".unicodeScalars { _ = EnglishComposition.process(key, composer: composer) }
        XCTAssertEqual(EnglishComposition.backspace(composer: composer),
                       .init(text: "", marked: true, handled: true))
        XCTAssertNil(EnglishComposition.backspace(composer: composer))
    }
    func testFinishingPreeditCommitsOnceBeforeModeChange() {
        let composer = composer()
        for scalar in "vn".unicodeScalars { _ = EnglishComposition.process(scalar, composer: composer) }
        XCTAssertEqual(composer.finishComposition(), "vn")
        XCTAssertEqual(composer.finishComposition(), "")
        composer.setEnabled(true)
        XCTAssertEqual(composer.buffer(), "")
        composer.process("a")
        composer.process("s")
        XCTAssertEqual(composer.finishComposition(), "á")
        composer.setEnabled(false)
        XCTAssertEqual(EnglishComposition.process(" ", composer: composer),
                       .init(text: " ", marked: false, handled: true))
    }

    func testEnglishEditingCommandsStayNative() {
        XCTAssertTrue(InputEventPolicy.passesThroughEnglish(keyCode: 51, modifiers: .option))
        XCTAssertTrue(InputEventPolicy.passesThroughEnglish(keyCode: 0, modifiers: .command))
        XCTAssertTrue(InputEventPolicy.passesThroughEnglish(keyCode: 0, modifiers: .control))
        XCTAssertTrue(InputEventPolicy.passesThroughEnglish(keyCode: 117, modifiers: .function))
        XCTAssertFalse(InputEventPolicy.passesThroughEnglish(keyCode: 51, modifiers: []))
        XCTAssertFalse(InputEventPolicy.passesThroughEnglish(keyCode: 0, modifiers: .option))
        XCTAssertFalse(InputEventPolicy.passesThroughEnglish(keyCode: 0, modifiers: [.shift, .capsLock]))
    }

}
