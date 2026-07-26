#if canImport(UIKit)
import ThemeSchema
import UIKit

@MainActor
final class EmojiSearchHeaderView: UIView {
    var onActivate: (() -> Void)?
    var onClear: (() -> Void)?
    var onCancel: (() -> Void)?

    private let surface = UIView()
    private let activateButton = UIButton(type: .system)
    private let iconView = UIImageView(image: UIImage(systemName: "magnifyingglass"))
    private let queryLabel = UILabel()
    private let clearButton = UIButton(type: .system)
    private let cancelButton = UIButton(type: .system)

    override init(frame: CGRect) {
        super.init(frame: frame)
        configure()
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let cancelWidth: CGFloat = cancelButton.isHidden ? 0 : 54
        surface.frame = CGRect(x: 8, y: 2, width: max(0, bounds.width - 16 - cancelWidth), height: bounds.height - 4)
        activateButton.frame = surface.bounds
        iconView.frame = CGRect(x: 12, y: (surface.bounds.height - 18) / 2, width: 18, height: 18)
        let clearWidth: CGFloat = clearButton.isHidden ? 0 : 34
        queryLabel.frame = CGRect(
            x: 38, y: 0, width: max(0, surface.bounds.width - 46 - clearWidth), height: surface.bounds.height
        )
        clearButton.frame = CGRect(x: surface.bounds.width - 36, y: 0, width: 36, height: surface.bounds.height)
        cancelButton.frame = CGRect(x: bounds.width - 58, y: 0, width: 54, height: bounds.height)
    }

    func apply(query: String, active: Bool, theme: ResolvedTheme, traits: UITraitCollection) {
        let label = theme.label.uiColor(for: traits)
        let secondary = theme.secondaryLabel.uiColor(for: traits)
        queryLabel.text = query.isEmpty ? "Tìm kiếm biểu tượng" : query
        queryLabel.textColor = query.isEmpty ? secondary : label
        iconView.tintColor = secondary
        clearButton.tintColor = secondary
        cancelButton.tintColor = theme.accent.uiColor(for: traits)
        surface.backgroundColor = theme.characterKey.uiColor(for: traits)
            .withAlphaComponent(max(0.5, CGFloat(theme.keyOpacity)))
        clearButton.isHidden = !active || query.isEmpty
        cancelButton.isHidden = !active
        activateButton.accessibilityValue = query.isEmpty ? nil : query
        setNeedsLayout()
    }

    private func configure() {
        surface.layer.cornerRadius = 12
        surface.layer.cornerCurve = .continuous
        iconView.contentMode = .scaleAspectFit
        queryLabel.font = .systemFont(ofSize: 15)
        clearButton.setImage(UIImage(systemName: "xmark.circle.fill"), for: .normal)
        clearButton.accessibilityLabel = "Xóa nội dung tìm kiếm"
        cancelButton.setTitle("Hủy", for: .normal)
        cancelButton.titleLabel?.font = .systemFont(ofSize: 15, weight: .medium)
        activateButton.accessibilityLabel = "Tìm kiếm biểu tượng"
        activateButton.addAction(UIAction { [weak self] _ in self?.onActivate?() }, for: .touchUpInside)
        clearButton.addAction(UIAction { [weak self] _ in self?.onClear?() }, for: .touchUpInside)
        cancelButton.addAction(UIAction { [weak self] _ in self?.onCancel?() }, for: .touchUpInside)
        addSubview(surface)
        surface.addSubview(activateButton)
        surface.addSubview(iconView)
        surface.addSubview(queryLabel)
        surface.addSubview(clearButton)
        addSubview(cancelButton)
    }
}
#endif
