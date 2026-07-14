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
            section(
                title: "Của bạn",
                identifier: "custom",
                themes: model.customThemes.map(\.theme),
                isCustom: true
            )
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
            carousel(themes: themes, isCustom: isCustom)
        }
    }

    @ViewBuilder private func carousel(
        themes: [KeyboardTheme],
        isCustom: Bool
    ) -> some View {
        if #available(iOS 26, *) {
            GlassEffectContainer(spacing: 16) {
                scroller(themes: themes, isCustom: isCustom)
            }
        } else {
            scroller(themes: themes, isCustom: isCustom)
        }
    }

    private func scroller(
        themes: [KeyboardTheme],
        isCustom: Bool
    ) -> some View {
        ScrollView(.horizontal) {
            HStack(spacing: 16) {
                if themes.isEmpty {
                    ThemeGalleryEmptyCard()
                } else {
                    ForEach(themes) { theme in themeCard(theme, isCustom: isCustom) }
                }
            }
            .scrollTargetLayout()
            .padding(.vertical, 4)
        }
        .scrollTargetBehavior(.viewAligned)
        .scrollIndicators(.hidden)
    }

    private func themeCard(_ theme: KeyboardTheme, isCustom: Bool) -> some View {
        ThemeCard(
            theme: theme,
            presentation: model.presentation(for: theme.id),
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
