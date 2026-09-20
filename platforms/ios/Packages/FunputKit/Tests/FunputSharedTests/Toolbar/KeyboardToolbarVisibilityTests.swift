import FunputShared
import KeyboardLayout
import Testing

/// The band carries the suggestions, the clipboard key and the emoji key, so it has to
/// outlive any single one of those features being switched off.
struct KeyboardToolbarVisibilityTests {
    @Test("The clipboard alone keeps the band, with its keys where they belong")
    func clipboardAloneKeepsTheBand() {
        var configuration = FunputConfiguration.default
        configuration.personalSuggestionsEnabled = false
        configuration.clipboardEnabled = true

        #expect(configuration.showsToolbar)
        let layout = resolve(configuration)
        #expect(layout.toolbar?.keys.map(\.role) == [.clipboard, .emoji])
        // The emoji key stays in the band: the toolbarless path, which moves it into the
        // action row, must not have run.
        #expect(!layout.rows.flatMap(\.keys).contains { $0.role == .emoji })
    }

    @Test("Suggestions alone keep the band")
    func suggestionsAloneKeepTheBand() {
        var configuration = FunputConfiguration.default
        configuration.personalSuggestionsEnabled = true
        configuration.clipboardEnabled = false

        #expect(configuration.showsToolbar)
        #expect(resolve(configuration).toolbar != nil)
    }

    @Test("Only switching both off returns the band to the rows")
    func bothOffDropsTheBand() {
        var configuration = FunputConfiguration.default
        configuration.personalSuggestionsEnabled = false
        configuration.clipboardEnabled = false

        #expect(!configuration.showsToolbar)
        let layout = resolve(configuration)
        #expect(layout.toolbar == nil)
        #expect(layout.rows.last?.keys.contains { $0.role == .emoji } == true)
    }

    private func resolve(_ configuration: FunputConfiguration) -> KeyboardLayout {
        KeyboardLayoutResolver.resolve(
            inputMethod: .telex,
            mode: .letters,
            preset: configuration.layoutPreset,
            showsToolbar: configuration.showsToolbar
        )
    }
}
