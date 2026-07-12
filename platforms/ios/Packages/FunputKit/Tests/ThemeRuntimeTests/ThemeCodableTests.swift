import Foundation
import Testing
import ThemeSchema
import ThemeRuntime

struct ThemeCodableTests {
    @Test("KeyboardTheme survives a JSON round-trip", arguments: BundledThemes.all)
    func roundTrip(_ theme: KeyboardTheme) throws {
        let data = try JSONEncoder().encode(theme)
        let decoded = try JSONDecoder().decode(KeyboardTheme.self, from: data)
        #expect(decoded == theme)
    }

    @Test("Bundled theme identifiers are unique")
    func uniqueIdentifiers() {
        let ids = BundledThemes.all.map(\.id)
        #expect(Set(ids).count == ids.count)
    }

    @Test("Lookup returns the matching bundled theme, or nil")
    func lookup() {
        #expect(BundledThemes.theme(id: BundledThemes.default.id) == .funputGlass)
        #expect(BundledThemes.theme(id: "does.not.exist") == nil)
    }
}
