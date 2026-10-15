/// Every page the settings open onto.
///
/// The keyboard tab lists ``hubRows``; ``data`` is reached from "Tôi", next to where an
/// account will live. Adding a page is a case here plus its view in
/// ``SettingsDestinationView`` — the rows, titles and navigation follow from it.
enum SettingsDestination: String, Hashable, CaseIterable, Identifiable {
    case typing
    case layout
    case smartInput
    case feedback
    case clipboard
    case data

    static let hubRows: [SettingsDestination] = [.typing, .layout, .smartInput, .feedback, .clipboard]

    var id: Self { self }

    var title: String {
        switch self {
        case .typing: "Gõ tiếng Việt"
        case .layout: "Bố cục và kích thước"
        case .smartInput: "Thông minh"
        case .feedback: "Phản hồi khi chạm"
        case .clipboard: "Clipboard"
        case .data: "Dữ liệu và khôi phục"
        }
    }

    var systemImage: String {
        switch self {
        case .typing: "character.bubble"
        case .layout: "keyboard"
        case .smartInput: "wand.and.stars"
        case .feedback: "hand.tap"
        case .clipboard: "clipboard"
        case .data: "externaldrive"
        }
    }
}
