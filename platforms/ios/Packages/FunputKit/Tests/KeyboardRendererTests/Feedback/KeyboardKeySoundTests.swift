#if canImport(UIKit)
import FunputShared
import KeyboardLayout
import Testing
@testable import KeyboardRenderer

/// Records what the keyboard asked to play, which a test bundle has no audio session for.
@MainActor
final class SoundSpy: KeyboardSoundPlaying {
    struct Played: Equatable {
        let kind: KeySoundKind
        let style: KeySoundStyle
        let volume: KeySoundVolume
    }

    private(set) var prepared: [KeySoundStyle] = []
    private(set) var played: [Played] = []

    func prepare(style: KeySoundStyle) { prepared.append(style) }
    func play(_ kind: KeySoundKind, style: KeySoundStyle, volume: KeySoundVolume) {
        played.append(Played(kind: kind, style: style, volume: volume))
    }
}

@MainActor
struct KeyboardKeySoundTests {
    @Test("Roles split into the system keyboard's three sounds", arguments: [
        (KeyRole.character, KeySoundKind.key), (.vniModifier, .key), (.punctuation, .key),
        (.backspace, .delete),
        (.space, .modifier), (.shift, .modifier), (.enter, .modifier), (.symbols, .modifier),
        (.globe, .modifier), (.capsLock, .modifier),
    ])
    func roleMapping(role: KeyRole, kind: KeySoundKind) {
        #expect(KeySoundKind(role: role) == kind)
    }

    @Test("A press plays its role's sound in the chosen style and volume")
    func pressPlaysSelection() {
        let subject = GestureTestSubject(key: KeySpec(id: "delete", label: "", role: .backspace))
        subject.soundStyle = .wood
        subject.soundVolume = .loud
        subject.begin()
        #expect(subject.sounds.played == [SoundSpy.Played(kind: .delete, style: .wood, volume: .loud)])
        #expect(subject.sounds.prepared.contains(.wood))
    }

    @Test("Off neither loads nor plays a sound")
    func offIsSilent() {
        let subject = GestureTestSubject(key: KeySpec(id: "a", label: "a", role: .character))
        subject.begin()
        #expect(subject.sounds.played.isEmpty)
        #expect(subject.sounds.prepared.isEmpty)
    }

    @Test("Placeholder keys make no sound")
    func placeholderIsSilent() {
        let subject = GestureTestSubject(key: KeySpec(id: "empty", label: "", role: .placeholder))
        subject.soundStyle = .system
        subject.begin()
        #expect(subject.sounds.played.isEmpty)
    }

    @Test("Suggestions play the key sound with the current settings")
    func suggestionSound() {
        let subject = GestureTestSubject(key: KeySpec(id: "a", label: "a", role: .character))
        subject.soundStyle = .bubble
        subject.soundVolume = .soft
        subject.controller.performSuggestionFeedback(presentation: subject.presentation)
        #expect(subject.sounds.played == [SoundSpy.Played(kind: .key, style: .bubble, volume: .soft)])
    }

    @Test("Every bundled style ships all three sounds")
    func bundledResources() {
        for style in KeySoundStyle.allCases where style != .system {
            for kind in KeySoundKind.allCases {
                #expect(KeySoundPlayerPool.resourceURL(style: style, kind: kind) != nil, "\(style)-\(kind)")
            }
        }
    }

    @Test("Volume levels rise and never exceed the media volume")
    func volumeGain() {
        let gains = KeySoundVolume.allCases.map(\.gain)
        #expect(gains == gains.sorted())
        #expect(Set(gains).count == gains.count)
        #expect(gains.allSatisfy { $0 > 0 && $0 <= 1 })
    }
}
#endif
