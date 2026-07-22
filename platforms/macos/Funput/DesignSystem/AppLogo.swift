import SwiftUI

/// The Funput app icon for hero / brand areas. It stays in the content layer;
/// non-interactive identity elements should not use Liquid Glass.
struct AppLogo: View {
    var size: CGFloat = 92
    var contentPadding: CGFloat = Theme.Spacing.md

    var body: some View {
        Image("FunputBrand")
            .resizable()
            .renderingMode(.original)
            .scaledToFit()
            .frame(width: size, height: size)
            .padding(contentPadding)
            .shadow(color: .black.opacity(0.12), radius: 12, y: 6)
    }
}
