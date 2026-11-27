import Combine
import KeyboardRenderer
import SwiftUI
import ThemeRuntime
import ThemeSchema

/// One row of the gallery.
enum ThemeGallerySection: String {
    case system
    case custom

    var title: String { self == .system ? "Hệ thống" : "Của bạn" }
    /// Identifies the row to the screen's scroll view and to UI tests.
    var scrollID: String { "appearance.section.\(rawValue)" }
}

extension AppearanceModel {
    /// The gallery row holding the previewed theme.
    var previewSection: ThemeGallerySection { previewCustomTheme == nil ? .system : .custom }
}

struct ThemeGallery: View {
    @ObservedObject var model: AppearanceModel
    /// Observed apart from the model so a newly captured picture redraws the cards.
    @ObservedObject var snapshots: ThemeSnapshotLibrary
    /// Called after a card was chosen for preview.
    let onPreview: () -> Void
    let onEdit: (String) -> Void
    let onDelete: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Label("Bộ sưu tập theme", systemImage: "paintpalette.fill")
                .font(.headline)
            Text("Vuốt ngang để xem các theme, chạm để xem trước.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            section(.system, themes: BundledThemes.all)
            if !model.customThemes.isEmpty {
                section(.custom, themes: model.customThemes.map(\.theme))
            }
        }
    }

    private func section(_ section: ThemeGallerySection, themes: [KeyboardTheme]) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(section.title)
                .font(.subheadline.weight(.semibold))
                .accessibilityIdentifier(section.scrollID)
            scroller(section, themes: themes)
        }
        .id(section.scrollID)
    }

    private func scroller(_ section: ThemeGallerySection, themes: [KeyboardTheme]) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHStack(spacing: 16) {
                ForEach(themes) { theme in themeCard(theme, isCustom: section == .custom) }
            }
            .padding(.vertical, 4)
            .modifier(ThemeGallerySnapContent())
        }
        .modifier(ThemeGallerySnapContainer())
        .accessibilityIdentifier("appearance.carousel.\(section.rawValue)")
    }

    private func themeCard(_ theme: KeyboardTheme, isCustom: Bool) -> some View {
        let presentation = model.presentation(for: theme.id)
        return ThemeCard(
            theme: theme,
            resolvedTheme: presentation.theme,
            backgroundImageData: model.imageData(for: theme),
            snapshot: isCustom ? model.snapshot(for: theme) : nil,
            interfaceStyle: model.previewMode.interfaceStyle,
            isPreviewed: theme.id == model.previewThemeID,
            isApplied: theme.id == model.appliedThemeID,
            isCustom: isCustom,
            editAction: { onEdit(theme.id) },
            deleteAction: { onDelete(theme.id) }
        ) {
            withAnimation(.easeInOut(duration: 0.2)) { model.selectTheme(theme.id) }
            onPreview()
        }
    }
}

private struct ThemeGallerySnapContent: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 17, *) {
            content.scrollTargetLayout()
        } else {
            content
        }
    }
}

private struct ThemeGallerySnapContainer: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 17, *) {
            content.scrollTargetBehavior(.viewAligned)
        } else {
            content
        }
    }
}
