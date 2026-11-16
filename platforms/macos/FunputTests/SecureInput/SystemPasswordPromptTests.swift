import XCTest
@testable import Funput

/// The lock screen, the authorization dialogs and Touch ID's password fallback get every key
/// raw without being probed: the probe misread their fields and typed the first letter of
/// the password twice.
@MainActor
final class SystemPasswordPromptTests: XCTestCase {
    private var secureInput = true

    private static let prompts = [
        "com.apple.loginwindow",
        "com.apple.LocalAuthenticationRemoteService",
        "com.apple.SecurityAgent",
        "com.apple.LocalAuthentication.UIAgent",
    ]

    private func makeGuard() -> SecureInputGuard {
        SecureInputGuard(secureInputEnabled: { [unowned self] in secureInput })
    }

    private func prompt(_ clientID: String = "com.apple.loginwindow") -> FakeCompositionSurface {
        let field = FakeCompositionSurface.passwordField()
        field.clientID = clientID
        return field
    }

    func testEveryPromptGetsItsFirstKeyRawWithoutAProbe() {
        for clientID in Self.prompts {
            var guarded = makeGuard()
            let field = prompt(clientID)
            XCTAssertEqual(guarded.decide(typing: "a", on: field), .handOver, clientID)
            XCTAssertEqual(field.calls, [], "\(clientID): nothing marked, nothing asked")
            XCTAssertEqual(field.document, "", "\(clientID): the app types the key itself")
        }
    }

    func testLaterKeysStayRaw() {
        var guarded = makeGuard()
        let field = prompt()
        _ = guarded.decide(typing: "a", on: field)
        XCTAssertEqual(guarded.decide(typing: "1", on: field), .handOver)
        XCTAssertEqual(guarded.decide(typing: "b", on: field, composing: true) { XCTFail("word ended") }, .handOver)
        XCTAssertEqual(guarded.decide(typing: nil, on: field), .handOver)
        XCTAssertEqual(field.calls, [])
    }

    func testAWordInProgressEndsBeforeTheKeyIsHandedOver() {
        var guarded = makeGuard()
        var ended = false
        XCTAssertEqual(guarded.decide(typing: "a", on: prompt(), composing: true) { ended = true }, .handOver)
        XCTAssertTrue(ended)
    }

    func testWithoutSecureInputAPromptIsLeftToTheUsualPath() {
        secureInput = false
        var guarded = makeGuard()
        let field = prompt()
        XCTAssertEqual(guarded.decide(typing: "a", on: field), .compose)
        XCTAssertEqual(field.calls, [])
    }

    func testAnotherClientIsProbedAgainAfterFocusMoves() {
        var guarded = makeGuard()
        _ = guarded.decide(typing: "a", on: prompt())
        guarded.reset()
        let field = FakeCompositionSurface()
        XCTAssertEqual(guarded.decide(typing: "t", on: field), .compose)
        XCTAssertEqual(field.calls, [.mark("t", nil), .markedRange, .mark("", nil)])
    }

    func testAClientThatDoesNotNameItselfIsProbed() {
        var guarded = makeGuard()
        let field = FakeCompositionSurface.passwordField()
        field.clientID = nil
        XCTAssertEqual(guarded.decide(typing: "a", on: field), .typed)
        XCTAssertEqual(field.calls, [.mark("a", nil), .markedRange])
    }
}
