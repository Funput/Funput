#if canImport(UIKit)
import KeyboardLayout
import UIKit

enum KeyboardKeyContentStyle {
    static func label(for spec: KeySpec, shiftState: ShiftState) -> String {
        guard spec.role == .character, shiftState.isUppercase else { return spec.label }
        return spec.shiftedLabel ?? spec.label.uppercased()
    }

    static func icon(for role: KeyRole, shiftState: ShiftState) -> UIImage? {
        let name: String?
        switch role {
        case .shift:
            name = switch shiftState {
            case .lowercase: "shift"
            case .uppercase: "shift.fill"
            case .capsLocked: "capslock.fill"
            }
        case .backspace:
            name = "delete.left"
        case .enter:
            name = "return"
        case .emoji:
            name = "face.smiling"
        case .capsLock:
            name = shiftState == .capsLocked ? "capslock.fill" : "capslock"
        case .globe:
            name = "globe"
        case .dismissKeyboard:
            name = "keyboard.chevron.compact.down"
        default:
            name = nil
        }
        return name.flatMap { UIImage(systemName: $0) }
    }

    /// The word a corner-labelled key shows; return names what it does, as the stock key does.
    static func cornerLabel(for spec: KeySpec, enterAction: KeyboardEnterAction) -> String {
        guard spec.role == .enter else { return spec.label }
        return switch enterAction {
        case .newLine: "return"
        case .go: "go"
        case .search: "search"
        case .send: "send"
        case .next: "next"
        case .done: "done"
        case .previous: "previous"
        case let .custom(label): label
        }
    }

    /// Shift and caps lock light up while they are on, since a word cannot fill in like an icon.
    static func isLit(_ role: KeyRole, shiftState: ShiftState) -> Bool {
        switch role {
        case .shift: shiftState.isUppercase
        case .capsLock: shiftState == .capsLocked
        default: false
        }
    }

    static func cornerFont(scale: Double) -> UIFont {
        .systemFont(ofSize: 17 * scale, weight: .regular)
    }

    static func font(for role: KeyRole, scale: Double) -> UIFont {
        let metrics: (size: CGFloat, weight: UIFont.Weight)
        switch role {
        case .character, .vniModifier, .punctuation:
            metrics = (22, .regular)
        case .space:
            metrics = (12, .medium)
        default:
            metrics = (14, .semibold)
        }
        return .systemFont(ofSize: metrics.size * scale, weight: metrics.weight)
    }
}
#endif
