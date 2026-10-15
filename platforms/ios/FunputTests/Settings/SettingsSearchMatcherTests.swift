import Testing
@testable import Funput

@MainActor
@Suite("Settings search")
struct SettingsSearchMatcherTests {
    private func titles(_ query: String) -> [String] {
        SettingsSearchMatcher.matches(query).map(\.title)
    }

    @Test("Accents and case do not matter")
    func accentInsensitive() {
        #expect(titles("rung").contains("Rung khi gõ"))
        #expect(titles("RUNG KHI GO").contains("Rung khi gõ"))
        #expect(titles("dat dau").contains("Kiểu đặt dấu"))
        #expect(titles("đặt dấu").contains("Kiểu đặt dấu"))
    }

    @Test("English keywords find Vietnamese settings")
    func englishKeywords() {
        #expect(titles("haptic").contains("Rung khi gõ"))
        #expect(titles("telex").contains("Kiểu gõ"))
        #expect(titles("reset").contains("Khôi phục cài đặt mặc định"))
    }

    @Test("Every word of the query must match")
    func allWordsMatch() {
        #expect(titles("am thanh").contains("Âm thanh khi gõ"))
        #expect(!titles("am thanh").contains("Rung khi gõ"))
        #expect(titles("rung zzz").isEmpty)
    }

    @Test("An empty or blank query finds nothing")
    func emptyQuery() {
        #expect(titles("").isEmpty)
        #expect(titles("   ").isEmpty)
    }

    @Test("The index covers every page and has no duplicates")
    func indexIsComplete() {
        let entries = SettingsSearchEntry.all
        #expect(Set(entries.map(\.id)).count == entries.count)
        #expect(Set(entries.map(\.destination)) == Set(SettingsDestination.allCases))
        // Every smart-input switch is findable by its own title.
        for toggle in SmartInputToggle.all {
            #expect(titles(toggle.title).contains(toggle.title))
        }
    }
}
