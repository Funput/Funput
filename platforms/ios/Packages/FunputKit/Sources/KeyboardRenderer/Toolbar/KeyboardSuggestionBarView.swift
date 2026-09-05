#if canImport(UIKit)
import ThemeSchema
import UIKit

@MainActor
final class KeyboardSuggestionBarView: UIView {
    var onSelection: ((KeyboardSuggestionCandidate) -> Void)?

    private static let maximumCount = 3
    private static let minimumWidth: CGFloat = 64
    /// Sized for the compact band: body text would fill it edge to edge, and a
    /// suggestion is a glance target rather than something the user reads. Dynamic Type
    /// still moves it, but only up to what the band can show without clipping the
    /// stacked Vietnamese diacritics.
    private static var labelFont: UIFont {
        UIFontMetrics(forTextStyle: .body).scaledFont(
            for: .systemFont(ofSize: 15),
            maximumPointSize: 17
        )
    }
    /// The divider's share of the band height, so it keeps its proportions at any
    /// keyboard size setting.
    private static let separatorInsetRatio: CGFloat = 0.24
    private let buttons = (0..<maximumCount).map { _ in UIButton(type: .system) }
    private let separators = (0..<(maximumCount - 1)).map { _ in UIView() }
    private var candidates: [KeyboardSuggestionCandidate] = []

    override init(frame: CGRect) {
        super.init(frame: frame)
        buttons.enumerated().forEach { index, button in
            button.tag = index
            button.titleLabel?.font = Self.labelFont
            button.titleLabel?.lineBreakMode = .byTruncatingTail
            button.accessibilityTraits = .keyboardKey
            button.addTarget(self, action: #selector(selectCandidate(_:)), for: .touchUpInside)
            addSubview(button)
        }
        separators.forEach(addSubview)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let count = min(candidates.count, Int(bounds.width / Self.minimumWidth))
        let width = count > 0 ? bounds.width / CGFloat(count) : 0
        for index in buttons.indices {
            let visible = index < count
            buttons[index].isHidden = !visible
            buttons[index].frame = visible
                ? CGRect(x: CGFloat(index) * width, y: 0, width: width, height: bounds.height)
                : .zero
        }
        let separatorInset = (bounds.height * Self.separatorInsetRatio).rounded()
        for index in separators.indices {
            let visible = index + 1 < count
            separators[index].isHidden = !visible
            separators[index].frame = visible
                ? CGRect(
                    x: CGFloat(index + 1) * width,
                    y: separatorInset,
                    width: 0.5,
                    height: bounds.height - separatorInset * 2
                )
                : .zero
        }
    }

    func update(_ candidates: [KeyboardSuggestionCandidate]) {
        self.candidates = Array(candidates.prefix(Self.maximumCount))
        buttons.enumerated().forEach { index, button in
            let candidate = self.candidates.indices.contains(index) ? self.candidates[index] : nil
            button.setTitle(candidate?.text, for: .normal)
            button.accessibilityLabel = candidate.map { "Gợi ý, \($0.text)" }
        }
        isHidden = self.candidates.isEmpty
        setNeedsLayout()
    }

    func apply(theme: ResolvedTheme, traits: UITraitCollection) {
        let label = theme.label.uiColor(for: traits)
        buttons.forEach { $0.setTitleColor(label, for: .normal) }
        separators.forEach { $0.backgroundColor = label.withAlphaComponent(0.18) }
    }

    @objc private func selectCandidate(_ sender: UIButton) {
        guard candidates.indices.contains(sender.tag) else { return }
        onSelection?(candidates[sender.tag])
    }
}
#endif
