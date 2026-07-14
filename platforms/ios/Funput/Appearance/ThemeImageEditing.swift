import SwiftUI
import ThemeSchema

extension ThemeEditorDraft {
    mutating func setBackgroundMode(_ mode: ThemeBackgroundMode) {
        customTheme.theme.backgroundEffects.mode = mode
    }

    mutating func installImage(_ image: ProcessedThemeImage) {
        let hadImage = customTheme.theme.backgroundEffects.image != nil
        sourceImageData = image.source
        renderedImageData = image.rendered
        needsAssetSave = true
        imageDataIsValid = true
        customTheme.theme.backgroundEffects.mode = .image
        customTheme.theme.backgroundEffects.image = ThemeBackgroundImage(assetID: "pending")
        if !hadImage {
            customTheme.theme.backgroundEffects.overlay = AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xFFFFFF, alpha: 0.15),
                dark: ThemeRGBA(hex: 0x000000, alpha: 0.25)
            )
        }
    }

    mutating func installRenderedImage(_ image: ProcessedThemeImage, blur: Double) {
        sourceImageData = image.source
        renderedImageData = image.rendered
        needsAssetSave = true
        imageDataIsValid = true
        customTheme.theme.backgroundEffects.image?.blurRadius = blur
    }

    mutating func removeImage() {
        customTheme.theme.backgroundEffects.mode = .gradient
        customTheme.theme.backgroundEffects.image = nil
        sourceImageData = nil
        renderedImageData = nil
        needsAssetSave = false
        imageDataIsValid = false
    }

    func overlayColor() -> Color {
        let adaptive = customTheme.theme.backgroundEffects.overlay
        let rgba = previewMode == .light ? adaptive.light : adaptive.dark
        return Color(.sRGB, red: rgba.red, green: rgba.green, blue: rgba.blue, opacity: 1)
    }

    mutating func setOverlayColor(_ color: Color) {
        let adaptive = customTheme.theme.backgroundEffects.overlay
        let old = previewMode == .light ? adaptive.light : adaptive.dark
        let uiColor = UIColor(color)
        var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0
        uiColor.getRed(&red, green: &green, blue: &blue, alpha: nil)
        let value = ThemeRGBA(red: red, green: green, blue: blue, alpha: old.alpha)
        customTheme.theme.backgroundEffects.overlay = previewMode == .light
            ? AdaptiveThemeColor(light: value, dark: adaptive.dark)
            : AdaptiveThemeColor(light: adaptive.light, dark: value)
    }

    func overlayOpacity() -> Double {
        let overlay = customTheme.theme.backgroundEffects.overlay
        return previewMode == .light ? overlay.light.alpha : overlay.dark.alpha
    }

    mutating func setOverlayOpacity(_ value: Double) {
        let overlay = customTheme.theme.backgroundEffects.overlay
        let old = previewMode == .light ? overlay.light : overlay.dark
        let replacement = ThemeRGBA(red: old.red, green: old.green, blue: old.blue, alpha: value)
        customTheme.theme.backgroundEffects.overlay = previewMode == .light
            ? AdaptiveThemeColor(light: replacement, dark: overlay.dark)
            : AdaptiveThemeColor(light: overlay.light, dark: replacement)
    }
}
