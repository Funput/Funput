import SwiftUI

struct ThemeImageOverlayControls: View {
    @Binding var draft: ThemeEditorDraft

    var body: some View {
        ContentCard {
            Text("Lớp phủ · \(draft.previewMode.title)").font(.headline)
            ColorPicker("Màu lớp phủ", selection: color, supportsOpacity: false)
                .accessibilityIdentifier("themeEditor.imageOverlayColor")
            ThemeMetricSlider(
                title: "Opacity lớp phủ",
                value: opacity,
                range: 0...1,
                step: 0.05,
                format: .percent
            )
            .accessibilityIdentifier("themeEditor.imageOverlayOpacity")
        }
    }

    private var color: Binding<Color> {
        Binding(get: { draft.overlayColor() }, set: { draft.setOverlayColor($0) })
    }

    private var opacity: Binding<Double> {
        Binding(get: { draft.overlayOpacity() }, set: { draft.setOverlayOpacity($0) })
    }
}
