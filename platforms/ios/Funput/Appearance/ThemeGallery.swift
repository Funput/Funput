import KeyboardRenderer
import SwiftUI
import ThemeRuntime
import ThemeSchema

struct ThemeGallery: View {
    let model: AppearanceModel
    let onEdit: (String) -> Void
    let onDelete: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Label("Bộ sưu tập theme", systemImage: "paintpalette.fill")
                .font(.headline)
            Text("Vuốt ngang để xem các theme, chạm để xem trước.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            section(
                title: "Hệ thống",
                identifier: "system",
                themes: BundledThemes.all,
                isCustom: false
            )
            if !model.customThemes.isEmpty {
                section(
                    title: "Của bạn",
                    identifier: "custom",
                    themes: model.customThemes.map(\.theme),
                    isCustom: true
                )
            }
        }
    }

    private func section(
        title: String,
        identifier: String,
        themes: [KeyboardTheme],
        isCustom: Bool
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .accessibilityIdentifier("appearance.section.\(identifier)")
            scroller(themes: themes, isCustom: isCustom, identifier: identifier)
        }
    }

    private func scroller(
        themes: [KeyboardTheme],
        isCustom: Bool,
        identifier: String
    ) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHStack(spacing: 16) {
                ForEach(themes) { theme in themeCard(theme, isCustom: isCustom) }
            }
            .scrollTargetLayout()
            .padding(.vertical, 4)
        }
        .scrollTargetBehavior(.viewAligned)
        .accessibilityIdentifier("appearance.carousel.\(identifier)")
    }

    private func themeCard(_ theme: KeyboardTheme, isCustom: Bool) -> some View {
        let presentation = model.presentation(for: theme.id)
        return ThemeCard(
            theme: theme,
            resolvedTheme: presentation.theme,
            backgroundImageData: model.imageData(for: theme),
            interfaceStyle: model.previewMode.interfaceStyle,
            isPreviewed: theme.id == model.previewThemeID,
            isApplied: theme.id == model.appliedThemeID,
            isCustom: isCustom,
            editAction: { onEdit(theme.id) },
            deleteAction: { onDelete(theme.id) }
        ) {
            withAnimation(.easeInOut(duration: 0.2)) { model.selectTheme(theme.id) }
        }
    }
}
