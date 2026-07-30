#if DEBUG
import Foundation
import FunputShared
import KeyboardInput
import KeyboardLayout
import Testing
@testable import Funput

@MainActor
struct ShadowTypingFixtureReplayTests {
    @Test("Checked-in reverse-encoder fixtures replay through production coordinator")
    func replayAllModes() {
        for fixture in ShadowTypingFixture.all {
            let coordinator = KeyboardInputCoordinator()
            coordinator.apply(
                ShadowTypingFixture.configuration(for: fixture.inputMethod)
            )
            let document = ScriptedWriter()
            for character in fixture.rawSequence {
                coordinator.handle(key(for: character), writer: document)
            }
            #expect(document.text == ShadowTypingFixture.expected)
        }
    }

    @Test("Advanced Telex uses shortcut forms")
    func advancedUsesShortcuts() throws {
        let advanced = try #require(
            ShadowTypingFixture.all.first { $0.inputMethod == .telexAdvanced }
        )
        let telex = try #require(
            ShadowTypingFixture.all.first { $0.inputMethod == .telex }
        )
        #expect(advanced.rawSequence != telex.rawSequence)
        #expect(advanced.rawSequence.contains("["))
        #expect(advanced.rawSequence.contains("]"))
    }

    private func key(for character: Character) -> KeySpec {
        let label = String(character)
        let role: KeyRole
        if character == " " {
            role = .space
        } else if character.isNumber {
            role = .vniModifier
        } else if character == "[" || character == "]" {
            role = .punctuation
        } else {
            role = .character
        }
        return KeySpec(
            id: "fixture-\(character)",
            label: label,
            role: role,
            shiftedLabel: label.uppercased()
        )
    }
}

#endif
