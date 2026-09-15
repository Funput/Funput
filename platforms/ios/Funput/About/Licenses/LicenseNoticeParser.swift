import Foundation

/// The few Markdown shapes the bundled `NOTICE.md` uses.
enum LicenseNoticeBlock: Equatable {
    case heading(String)
    /// Prose that may carry inline Markdown such as links.
    case paragraph(String)
    /// Licence text quoted from a fenced block, never parsed as Markdown.
    case quote(String)
}

/// Turns `NOTICE.md` into blocks whose hard-wrapped lines reflow to the screen width.
enum LicenseNoticeParser {
    static func blocks(from markdown: String) -> [LicenseNoticeBlock] {
        var builder = LicenseNoticeBuilder()
        for line in markdown.components(separatedBy: .newlines) {
            builder.consume(line)
        }
        builder.endParagraph()
        return builder.blocks
    }
}

private struct LicenseNoticeBuilder {
    var blocks: [LicenseNoticeBlock] = []
    private var lines: [String] = []
    private var insideFence = false

    mutating func consume(_ rawLine: String) {
        let line = rawLine.trimmingCharacters(in: .whitespaces)
        if line.hasPrefix("```") {
            // Closing the paragraph first files it under the side of the fence it sat on.
            endParagraph()
            insideFence.toggle()
        } else if line.isEmpty {
            endParagraph()
        } else if !insideFence, line.hasPrefix("#") {
            endParagraph()
            blocks.append(.heading(String(line.drop { $0 == "#" || $0 == " " })))
        } else {
            lines.append(line)
        }
    }

    mutating func endParagraph() {
        guard !lines.isEmpty else { return }
        let text = lines.joined(separator: " ")
        blocks.append(insideFence ? .quote(text) : .paragraph(text))
        lines.removeAll()
    }
}
