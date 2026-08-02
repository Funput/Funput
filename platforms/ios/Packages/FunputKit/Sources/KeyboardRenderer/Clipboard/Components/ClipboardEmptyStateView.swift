#if canImport(UIKit)
import ThemeSchema
import UIKit

/// Why the list is empty, said plainly.
///
/// A bare empty list reads as a broken feature, and the two reasons it can be empty
/// call for different things from the user.
public enum ClipboardEmptyState: Equatable, Sendable {
    /// The pasteboard is unreadable without it, so there is nothing to show at all.
    case needsFullAccess
    case nothingSaved

    var title: String {
        switch self {
        case .needsFullAccess: "Cần Cho phép truy cập đầy đủ"
        case .nothingSaved: "Chưa có gì trong lịch sử"
        }
    }

    var detail: String {
        switch self {
        case .needsFullAccess:
            "Bật trong Cài đặt › Bàn phím › Funput để dùng lịch sử clipboard."
        case .nothingSaved:
            // The limit of never reading the pasteboard behind the user's back: only
            // what they pasted through Funput can be here. Better said up front than
            // discovered as a missing feature.
            "Sao chép văn bản rồi chạm nút Dán trên thanh công cụ. Lịch sử chỉ lưu những gì bạn đã dán qua Funput."
        }
    }
}

@MainActor
final class ClipboardEmptyStateView: UIView {
    private let titleLabel = UILabel()
    private let detailLabel = UILabel()

    override init(frame: CGRect) {
        super.init(frame: frame)
        titleLabel.font = .systemFont(ofSize: 15, weight: .medium)
        titleLabel.textAlignment = .center
        detailLabel.font = .systemFont(ofSize: 13)
        detailLabel.textAlignment = .center
        detailLabel.numberOfLines = 3
        [titleLabel, detailLabel].forEach(addSubview)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let inset: CGFloat = 24
        let width = max(0, bounds.width - inset * 2)
        let detailHeight = detailLabel.sizeThatFits(
            CGSize(width: width, height: .greatestFiniteMagnitude)
        ).height
        let total = 22 + 6 + detailHeight
        let top = max(0, (bounds.height - total) / 2)
        titleLabel.frame = CGRect(x: inset, y: top, width: width, height: 22)
        detailLabel.frame = CGRect(
            x: inset, y: titleLabel.frame.maxY + 6, width: width, height: detailHeight
        )
    }

    func apply(state: ClipboardEmptyState, theme: ResolvedTheme, traits: UITraitCollection) {
        titleLabel.text = state.title
        titleLabel.textColor = theme.label.uiColor(for: traits)
        detailLabel.text = state.detail
        detailLabel.textColor = theme.secondaryLabel.uiColor(for: traits)
        setNeedsLayout()
    }
}
#endif
