import Foundation

/// Turns a stored clip into the two strings a row shows.
///
/// The relative time is built by hand rather than with `RelativeDateTimeFormatter`:
/// the wording then cannot drift with OS locale data, a test can pin it exactly, and
/// no formatter has to be allocated or kept alive per row.
enum ClipboardRowText {
    static let previewLimit = 120

    /// Collapses every run of whitespace — newlines included — into one space.
    ///
    /// The row shows at most two lines, so a clip that starts with blank lines would
    /// otherwise render as an empty row. Structure is lost, but the point here is to
    /// recognise the clip, not to read it.
    static func preview(_ text: String) -> String {
        let collapsed = text
            .split(whereSeparator: \.isWhitespace)
            .joined(separator: " ")
        guard collapsed.count > previewLimit else { return collapsed }
        return collapsed.prefix(previewLimit) + "…"
    }

    static func relativeTime(from date: Date, now: Date = Date()) -> String {
        let seconds = Int(now.timeIntervalSince(date))
        guard seconds >= 60 else { return "Vừa xong" }
        let minutes = seconds / 60
        guard minutes >= 60 else { return "\(minutes) phút trước" }
        let hours = minutes / 60
        guard hours >= 24 else { return "\(hours) giờ trước" }
        return "\(hours / 24) ngày trước"
    }
}
