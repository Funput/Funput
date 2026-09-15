import Foundation
import Testing

struct LexiconNoticeTests {
    @Test func appContainsCompleteSourceNotices() throws {
        let url = try #require(Bundle.main.url(forResource: "NOTICE", withExtension: "md"))
        let text = try String(contentsOf: url, encoding: .utf8)
        for expected in [
            "Copyright 2000-2026 by Kevin Atkinson",
            "without express or implied warranty",
            "Google Books Ngram Viewer",
            "Creative Commons Attribution 3.0 Unported",
            "Creative Commons Attribution 4.0 International",
            "List of Dirty, Naughty, Obscene, and Otherwise Bad Words",
            "MIT licence",
        ] {
            #expect(text.contains(expected))
        }
    }
}
