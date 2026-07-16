import XCTest

@MainActor
final class ReverseReleaseRolloverTouchPipelineTests: XCTestCase {
    func testFullParagraphCommitsInPressOrder() throws {
        let stack = try RolloverTypingTestStack.make()
        let characters = Array(RolloverTypingFixture.keys)

        for index in stride(from: 0, to: characters.count, by: 2) {
            let first = try XCTUnwrap(stack.interaction[characters[index]])
            first.sendActions(for: .touchDown)
            guard index + 1 < characters.count else {
                first.sendActions(for: .touchUpInside)
                break
            }
            let second = try XCTUnwrap(stack.interaction[characters[index + 1]])
            second.sendActions(for: .touchDown)
            second.sendActions(for: .touchUpInside)
            first.sendActions(for: .touchUpInside)
        }

        XCTAssertEqual(
            stack.document.text,
            RolloverTypingFixture.expected,
            RolloverTypingFixture.diff(stack.document.text)
        )
    }
}
