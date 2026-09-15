import Foundation
@testable import Funput
import Testing

@MainActor
struct LicenseNoticeParserTests {
    @Test func reflowsProseAndKeepsQuotedLicenceOutOfMarkdown() {
        let markdown = """
        # Title

        First line of a
        hard-wrapped paragraph with a <https://example.com> link.

        ```text
        Copyright 2000 by Someone

        Permission to use,
        # not a heading
        ```

        After the quote.
        """
        #expect(LicenseNoticeParser.blocks(from: markdown) == [
            .heading("Title"),
            .paragraph("First line of a hard-wrapped paragraph with a <https://example.com> link."),
            .quote("Copyright 2000 by Someone"),
            .quote("Permission to use, # not a heading"),
            .paragraph("After the quote."),
        ])
    }

    @Test func bundledNoticeHasNoLeftoverMarkupOrHardWraps() throws {
        let url = try #require(Bundle.main.url(forResource: "NOTICE", withExtension: "md"))
        let blocks = LicenseNoticeParser.blocks(from: try String(contentsOf: url, encoding: .utf8))
        let headings = blocks.compactMap { block -> String? in
            if case .heading(let text) = block { text } else { nil }
        }
        #expect(headings.contains("SCOWL / English Speller Database (ESDB)"))
        #expect(headings.contains("Funput"))
        #expect(blocks.contains { if case .quote = $0 { true } else { false } })
        for block in blocks {
            let text = switch block {
            case .heading(let value), .paragraph(let value), .quote(let value): value
            }
            #expect(!text.contains("```") && !text.contains("\n"))
        }
    }
}
