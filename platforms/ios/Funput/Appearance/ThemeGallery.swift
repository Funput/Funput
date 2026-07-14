import SwiftUI
import ThemeRuntime
import ThemeSchema

struct ThemeGallery: View {
    let model: AppearanceModel
    let onEdit: (String) -> Void
    let onDelete: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Bộ sưu tập theme", systemImage: "paintpalette.fill")
                .font(.headline)
            Text("Vuốt ngang để xem các theme, chạm để xem trước.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            carousel
        }
    }

    @ViewBuilder private var carousel: some View {
        if #available(iOS 26, *) {
            GlassEffectContainer(spacing: 16) { scroller }
        } else {
            scroller
        }
    }

    private var scroller: some View {
        ScrollView(.horizontal) {
            HStack(spacing: 16) {
                ForEach(model.themes) { theme in
                    ThemeCard(
                        theme: theme,
                        presentation: model.presentation(for: theme.id),
                        backgroundImageData: model.imageData(for: theme),
                        interfaceStyle: model.previewMode.interfaceStyle,
                        isPreviewed: theme.id == model.previewThemeID,
                        isApplied: theme.id == model.appliedThemeID,
                        isCustom: model.catalog.customTheme(id: theme.id) != nil,
                        editAction: { onEdit(theme.id) },
                        deleteAction: { onDelete(theme.id) }
                    ) {
                        withAnimation(.easeInOut(duration: 0.2)) { model.selectTheme(theme.id) }
                    }
                }
            }
            .scrollTargetLayout()
            .padding(.vertical, 4)
        }
        .scrollTargetBehavior(.viewAligned)
        .scrollIndicators(.hidden)
    }
}
