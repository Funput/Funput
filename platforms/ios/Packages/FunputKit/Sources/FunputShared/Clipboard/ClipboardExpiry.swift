import Foundation

/// How long an unpinned clip stays in the history.
///
/// Short by default: whatever passes through the pasteboard passes through this
/// store too, passwords and one-time codes included. Anything worth keeping longer
/// can be pinned, and a pinned entry never expires whatever this says.
public enum ClipboardExpiry: String, CaseIterable, Codable, Hashable, Sendable {
    case hour
    case day
    case week

    public var interval: TimeInterval {
        switch self {
        case .hour: 60 * 60
        case .day: 24 * 60 * 60
        case .week: 7 * 24 * 60 * 60
        }
    }

    public var title: String {
        switch self {
        case .hour: "1 giờ"
        case .day: "1 ngày"
        case .week: "1 tuần"
        }
    }

    public var summary: String {
        switch self {
        case .hour: "An toàn nhất — mật khẩu lỡ sao chép không nằm lại lâu."
        case .day: "Giữ qua một ngày làm việc."
        case .week: "Giữ lâu nhất. Cân nhắc nếu bạn hay sao chép thông tin nhạy cảm."
        }
    }
}
