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
        case .systemInputMode:
            name = "globe"
        case .enter:
            name = "return"
        case .emoji:
            name = "face.smiling"
        case .settings:
            name = "gearshape"
        default:
            name = nil
        }
        return name.flatMap { UIImage(systemName: $0) }
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
