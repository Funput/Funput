#if canImport(UIKit)
import UIKit

/// Whether a text field is one typo correction may touch at all.
///
/// Decided from the traits the field declares, before the first key: a field that
/// asked for no autocorrect gets none, an ASCII-only field is one where the user is
/// typing something that is not a word (a code, a handle, a key), and a field whose
/// content type names a credential or an address holds text no repair should ever
/// rewrite. The system keyboard draws the same lines.
public enum CorrectionFieldPolicy {
    /// Content types whose text is not prose. Repairing a password is not a repair.
    static let protectedContentTypes: Set<UITextContentType> = [
        .username, .password, .newPassword, .oneTimeCode, .URL, .emailAddress,
    ]

    public static func allowsAutocorrect(
        autocorrectionType: UITextAutocorrectionType,
        keyboardType: UIKeyboardType,
        textContentType: UITextContentType?
    ) -> Bool {
        guard autocorrectionType != .no else { return false }
        switch keyboardType {
        case .asciiCapable, .asciiCapableNumberPad:
            return false
        default:
            break
        }
        guard let textContentType else { return true }
        return !protectedContentTypes.contains(textContentType)
    }
}
#endif
