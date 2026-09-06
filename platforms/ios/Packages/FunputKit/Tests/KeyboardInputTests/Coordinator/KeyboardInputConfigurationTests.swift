#if os(iOS) && canImport(FunputCore)
import Foundation
import Testing
import FunputShared
import KeyboardInput
import KeyboardLayout

@MainActor
struct KeyboardInputConfigurationTests {
    @Test("Applying configuration updates input method and language")
    func applyUpdatesState() {
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(FunputConfiguration(inputMethod: .telex, language: .english))
        #expect(coordinator.state.inputMethod == .telex)
        #expect(coordinator.state.language == .english)
    }

    @Test("Telex configuration composes Vietnamese")
    func telexComposes() {
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(FunputConfiguration(inputMethod: .telex))
        let document = TestKeyboardWriter()
        type("as", with: coordinator, into: document)
        #expect(document.text == "á")
    }

    @Test("VNI configuration composes tone modifiers")
    func vniComposes() {
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(FunputConfiguration(inputMethod: .vni))
        let document = TestKeyboardWriter()
        type("ma", with: coordinator, into: document)
        type("1", role: .vniModifier, with: coordinator, into: document)
        #expect(document.text == "má")
    }

    @Test("English configuration disables Vietnamese composition")
    func englishPassthrough() {
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(FunputConfiguration(inputMethod: .telex, language: .english))
        let document = TestKeyboardWriter()
        type("as", with: coordinator, into: document)
        #expect(document.text == "as")
    }

    @Test("Default configuration uses modern tone placement")
    func defaultUsesModernTone() {
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(.default)
        let document = TestKeyboardWriter()
        type("hoa", with: coordinator, into: document)
        type("2", role: .vniModifier, with: coordinator, into: document)
        #expect(document.text == "hoà")
    }

    @Test("Explicit tone style controls placement", arguments: [ToneStyleOption.traditional, .modern])
    func explicitToneStyle(style: ToneStyleOption) {
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(FunputConfiguration(inputMethod: .vni, toneStyle: style))
        let document = TestKeyboardWriter()
        type("hoa", with: coordinator, into: document)
        type("2", role: .vniModifier, with: coordinator, into: document)
        #expect(document.text == (style == .traditional ? "hòa" : "hoà"))
    }

    @Test("Legacy configuration without tone style keeps traditional placement")
    func legacyUsesTraditionalTone() throws {
        let configuration = try JSONDecoder().decode(FunputConfiguration.self, from: Data("{}".utf8))
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(configuration)
        let document = TestKeyboardWriter()
        type("hoa", with: coordinator, into: document)
        type("2", role: .vniModifier, with: coordinator, into: document)
        #expect(document.text == "hòa")
    }
}
#endif
