import SwiftUI
import ThemeSchema
import UIKit

/// A lightweight gallery representation. The selected theme is rendered by the
/// production keyboard preview above the gallery; cards only need to communicate
/// palette and surface hierarchy without mounting another keyboard renderer.
struct ThemeCardThumbnail: View {
    let theme: ResolvedTheme
    let backgroundImageData: Data?
    let interfaceStyle: UIUserInterfaceStyle

    private let rowCounts = [10, 9, 7]

    var body: some View {
        ZStack {
            background
            VStack(spacing: 7) {
                toolbar
                ForEach(rowCounts.indices, id: \.self) { row in
                    keyRow(row, count: rowCounts[row])
                }
            }
            .padding(12)
        }
        .clipShape(.rect(cornerRadius: 16))
        .overlay {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(
                    color(theme.border).opacity(theme.borderWidth > 0 ? 0.7 : 0),
                    lineWidth: 1
                )
        }
    }

    @ViewBuilder private var background: some View {
        if let data = backgroundImageData, let image = UIImage(data: data) {
            Image(uiImage: image)
                .resizable()
                .scaledToFill()
        } else {
            LinearGradient(
                colors: [color(theme.backgroundStart), color(theme.backgroundEnd)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        }
    }

    private var toolbar: some View {
        HStack(spacing: 6) {
            Capsule().fill(color(theme.accent)).frame(width: 28, height: 7)
            Spacer()
            Circle().fill(color(theme.specialKey)).frame(width: 12, height: 12)
            Circle().fill(color(theme.specialKey)).frame(width: 12, height: 12)
        }
        .frame(height: 18)
    }

    private func keyRow(_ row: Int, count: Int) -> some View {
        HStack(spacing: 5) {
            ForEach(0..<count, id: \.self) { index in
                let special = row == rowCounts.count - 1 && (index == 0 || index == count - 1)
                RoundedRectangle(cornerRadius: 5, style: .continuous)
                    .fill(color(special ? theme.specialKey : theme.characterKey))
                    .opacity(special ? theme.specialKeyOpacity : theme.keyOpacity)
                    .frame(maxWidth: .infinity, minHeight: 30)
            }
        }
    }

    private func color(_ adaptive: AdaptiveThemeColor) -> Color {
        let value = interfaceStyle == .dark ? adaptive.dark : adaptive.light
        return Color(red: value.red, green: value.green, blue: value.blue, opacity: value.alpha)
    }
}
