import SwiftUI
import ThemeSchema
import UIKit

/// A lightweight gallery representation. Bundled themes show artwork captured from the
/// production renderer (``BundledThemeThumbnail``), custom themes a picture taken from a
/// preview on screen (``ThemeSnapshotLibrary``). Without either, the card falls back to
/// a drawn sketch of palette and surfaces. None mounts another keyboard renderer.
struct ThemeCardThumbnail: View {
    let themeID: String
    let theme: ResolvedTheme
    let backgroundImageData: Data?
    var snapshot: UIImage?
    let interfaceStyle: UIUserInterfaceStyle

    private let rowCounts = [10, 9, 7]

    var body: some View {
        if let artwork = BundledThemeThumbnail.image(themeID: themeID, theme: theme, style: interfaceStyle) {
            picture(artwork, contentMode: .fill)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        } else if let snapshot {
            // A capture keeps the phone's own keyboard proportions, which rarely match
            // the card, so it is fitted whole over a blurred fill of itself.
            ZStack {
                picture(snapshot, contentMode: .fill).blur(radius: 18, opaque: true)
                picture(snapshot, contentMode: .fit)
            }
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        } else {
            sketch
        }
    }

    private func picture(_ image: UIImage, contentMode: ContentMode) -> some View {
        Image(uiImage: image)
            .resizable()
            .aspectRatio(contentMode: contentMode)
            .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity)
    }

    /// The background sits in `.background` so it takes the keys' frame instead of setting
    /// it: a fill-scaled photo reports its own aspect, and as a ZStack sibling a portrait
    /// photo stretched the whole sketch past the card (#805).
    private var sketch: some View {
        VStack(spacing: 7) {
            toolbar
            ForEach(rowCounts.indices, id: \.self) { row in
                keyRow(row, count: rowCounts[row])
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background { background }
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
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
