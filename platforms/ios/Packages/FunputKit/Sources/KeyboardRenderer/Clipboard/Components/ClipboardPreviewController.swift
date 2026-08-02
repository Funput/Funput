#if canImport(UIKit)
import UIKit

/// The full text of one clip, shown as the preview of a row's context menu.
///
/// Rows are one line, so a long clip is unrecognisable from its preview alone.
/// Long-press already opened a menu; this fills in the card above it rather than
/// adding another gesture for the user to discover.
///
/// Deliberately styled with system colours, not the keyboard theme: the card is
/// system chrome floating on its own material, so a theme's white label text would
/// vanish on it.
@MainActor
final class ClipboardPreviewController: UIViewController {
    /// Kept inside the keyboard's own height — a keyboard extension has no room to
    /// float a tall card, and an oversized one gets clipped.
    static let maximumHeight: CGFloat = 240
    static let minimumWidth: CGFloat = 260
    private static let inset: CGFloat = 14

    let textView = UITextView()
    private let text: String
    private let width: CGFloat

    init(text: String, width: CGFloat) {
        self.text = text
        self.width = max(Self.minimumWidth, width)
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        // The font is assigned before anything reads it: a fresh `UITextView` has a
        // nil font, and this runs inside a keyboard extension where a crash drops the
        // user straight back to the system keyboard.
        let font = UIFont.systemFont(ofSize: 15)
        view.backgroundColor = .clear
        textView.font = font
        textView.text = text
        textView.isEditable = false
        // A context-menu preview does not deliver gestures to its content: iOS treats
        // the card as static. Scrolling here would silently do nothing, so anything
        // past the card's height is truncated with an ellipsis instead of pretending.
        textView.isScrollEnabled = false
        textView.textContainer.lineBreakMode = .byTruncatingTail
        textView.textContainer.maximumNumberOfLines = Self.lineLimit(for: font)
        textView.backgroundColor = .clear
        textView.textColor = .label
        textView.textContainerInset = UIEdgeInsets(
            top: Self.inset, left: Self.inset, bottom: Self.inset, right: Self.inset
        )
        view.addSubview(textView)
        preferredContentSize = CGSize(width: width, height: fittingHeight)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        textView.frame = view.bounds
    }

    private static func lineLimit(for font: UIFont) -> Int {
        max(1, Int((maximumHeight - inset * 2) / font.lineHeight))
    }

    private var fittingHeight: CGFloat {
        let fitted = textView.sizeThatFits(
            CGSize(width: width, height: .greatestFiniteMagnitude)
        ).height
        return min(max(fitted, 44), Self.maximumHeight)
    }
}
#endif
