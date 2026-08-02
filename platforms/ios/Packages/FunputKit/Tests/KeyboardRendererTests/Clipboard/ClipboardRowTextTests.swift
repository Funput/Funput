import Foundation
@testable import KeyboardRenderer
import Testing

@Suite("Clipboard row text")
struct ClipboardRowTextTests {
    private let now = Date(timeIntervalSince1970: 1_800_000_000)

    /// A clip that opens with blank lines would otherwise render as an empty row.
    @Test("Preview collapses every run of whitespace into one space")
    func collapsesWhitespace() {
        #expect(ClipboardRowText.preview("\n\n  xin   chào\n\tbạn  \n") == "xin chào bạn")
    }

    @Test("Preview truncates past the limit and marks it")
    func truncates() {
        let long = String(repeating: "a", count: ClipboardRowText.previewLimit + 40)
        let preview = ClipboardRowText.preview(long)
        #expect(preview.count == ClipboardRowText.previewLimit + 1)
        #expect(preview.hasSuffix("…"))
    }

    @Test("Preview leaves a short single-line clip alone")
    func shortText() {
        #expect(ClipboardRowText.preview("xin chào") == "xin chào")
    }

    /// Boundaries included on both sides, since the wording is hand-rolled precisely
    /// so it cannot drift with OS locale data.
    static let relativeCases: [(age: TimeInterval, expected: String)] = [
        (0, "Vừa xong"),
        (59, "Vừa xong"),
        (60, "1 phút trước"),
        (3_540, "59 phút trước"),
        (3_600, "1 giờ trước"),
        (82_800, "23 giờ trước"),
        (86_400, "1 ngày trước"),
        (259_200, "3 ngày trước"),
    ]

    @Test("Relative time reads in Vietnamese at every bucket", arguments: relativeCases)
    func relativeTime(_ testCase: (age: TimeInterval, expected: String)) {
        let captured = now.addingTimeInterval(-testCase.age)
        #expect(ClipboardRowText.relativeTime(from: captured, now: now) == testCase.expected)
    }
}
