#if canImport(UIKit)
import UIKit
#endif

/// Which appearance the keyboard renders in, independent of the host app.
///
/// A keyboard extension inherits its trait collection from whichever app is being
/// typed into, so an app that pins itself to light mode also pins the keyboard.
/// This option lets the user override that.
public enum KeyboardAppearanceOption: String, CaseIterable, Hashable, Sendable, Codable {
    /// Follow the host app's trait collection — the behaviour before this option existed.
    case system
    case light
    case dark
}

#if canImport(UIKit)
public extension KeyboardAppearanceOption {
    var interfaceStyle: UIUserInterfaceStyle {
        switch self {
        case .system: .unspecified
        case .light: .light
        case .dark: .dark
        }
    }
}
#endif
