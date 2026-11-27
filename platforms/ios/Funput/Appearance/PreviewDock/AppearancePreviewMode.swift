import UIKit

enum AppearancePreviewMode: String, CaseIterable, Identifiable {
    case light
    case dark

    var id: String { rawValue }
    var title: String { self == .light ? "Sáng" : "Tối" }
    var interfaceStyle: UIUserInterfaceStyle { self == .light ? .light : .dark }
}
