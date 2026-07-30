import Foundation
import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import PersonalSuggestions
import Testing
import UIKit

@MainActor
@Suite("Personal suggestions iOS integration")
struct PersonalSuggestionIntegrationTests {
    @Test("Rust bridge learns and returns UTF-32 candidates")
    func bridge() throws {
        let engine = try #require(PersonalSuggestionEngine.inMemory())
        #expect(engine.learn("không"))
        #expect(engine.learn("không"))
        #expect(engine.query("kh").map(\.text) == ["không"])
        #expect(engine.stats().promotedWords == 1)
    }

    @Test("Persistent bridge flushes reopens and resets")
    func persistence() throws {
        let root = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        defer { try? FileManager.default.removeItem(at: root) }
        do {
            let engine = try #require(PersonalSuggestionEngine.open(storeURL: root))
            #expect(engine.learn("chào"))
            #expect(engine.learn("chào"))
            #expect(engine.flush())
        }
        let reopened = try #require(PersonalSuggestionEngine.open(storeURL: root))
        #expect(reopened.query("ch").map(\.text) == ["chào"])
        #expect(reopened.reset())
        #expect(reopened.query("ch").isEmpty)
    }

    @Test("Authored token completes and candidate inserts one space")
    func tokenAndAcceptance() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = ScriptedWriter()
        type("ban", coordinator: coordinator, writer: document)
        #expect(coordinator.takePersonalSuggestionUpdate().prefix == "ban")
        #expect(
            coordinator.acceptSuggestion("bạn", replacing: "ban", writer: document) != nil
        )
        #expect(document.text == "bạn ")
        #expect(coordinator.takePersonalSuggestionUpdate().completedToken == "bạn")
    }

    @Test("External autocorrection resets without learning host text")
    func autocorrectionIsolation() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = ScriptedWriter()
        type("ban", coordinator: coordinator, writer: document)
        document.replaceTextExternally(with: "bank")
        coordinator.synchronizeDocument(document, event: .textChanged)
        #expect(coordinator.takePersonalSuggestionUpdate() == .empty)
    }

    @Test("Decomposed combining marks remain part of the authored token")
    func decomposedToken() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = ScriptedWriter()
        type("A\u{0301}n", coordinator: coordinator, writer: document)
        #expect(coordinator.takePersonalSuggestionUpdate().prefix == "A\u{0301}n")
    }

    @Test("Toolbar keeps controls and emits candidate exactly once")
    func toolbar() {
        let surface = KeyboardSurfaceView()
        surface.frame = CGRect(x: 0, y: 0, width: 390, height: 300)
        surface.layoutIfNeeded()
        let controlsBefore = controls(in: surface).map { ObjectIdentifier($0) }
        let candidate = KeyboardSuggestionCandidate(text: "không", generation: 9)
        var selected: [KeyboardSuggestionCandidate] = []
        surface.onSuggestionSelected = { selected.append($0) }
        surface.updateSuggestions([candidate])
        surface.layoutIfNeeded()
        let button = buttons(in: surface).first { $0.accessibilityLabel == "Gợi ý, không" }
        #expect(button != nil)
        button?.sendActions(for: .touchUpInside)
        #expect(selected == [candidate])
        #expect(controls(in: surface).map { ObjectIdentifier($0) } == controlsBefore)
    }

    private func type(
        _ text: String,
        coordinator: KeyboardInputCoordinator,
        writer: ScriptedWriter
    ) {
        for character in text {
            coordinator.handle(
                KeySpec(id: "key-\(character)", label: String(character), role: .character),
                writer: writer
            )
        }
    }

    private func controls(in view: UIView) -> [UIControl] {
        view.subviews.flatMap { child in
            ((child as? UIControl).map { [$0] } ?? []) + controls(in: child)
        }
    }

    private func buttons(in view: UIView) -> [UIButton] {
        view.subviews.flatMap { child in
            ((child as? UIButton).map { [$0] } ?? []) + buttons(in: child)
        }
    }
}
