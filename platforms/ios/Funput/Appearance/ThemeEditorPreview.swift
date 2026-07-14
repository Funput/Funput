import KeyboardRenderer
import SwiftUI

struct ThemeEditorPreview: View {
    @Binding var draft: ThemeEditorDraft
    let presentation: KeyboardPresentation
    let availableHeight: CGFloat

    var body: some View {
        VStack(spacing: 10) {
            HStack(spacing: 12) {
                Label("Xem trước", systemImage: "keyboard")
                    .font(.headline)
                Spacer()
                Picker("Chế độ xem trước", selection: $draft.previewMode) {
                    ForEach(AppearancePreviewMode.allCases) { mode in
                        Text(mode.title).tag(mode)
                    }
                }
                .pickerStyle(.segmented)
                .frame(maxWidth: 190)
                .accessibilityIdentifier("appearance.previewMode")
            }
            KeyboardPreview(
                presentation: presentation,
                interfaceStyle: draft.previewMode.interfaceStyle,
                isInteractive: true
            )
            .id(draft.previewMode)
            .frame(height: previewHeight)
            .clipShape(.rect(cornerRadius: 18))
            .shadow(color: .black.opacity(0.14), radius: 12, y: 6)
            .accessibilityIdentifier("themeEditor.preview")
        }
        .padding(.horizontal, 18)
        .padding(.top, 10)
        .padding(.bottom, 12)
    }

    private var previewHeight: CGFloat {
        let natural = KeyboardMetrics.phonePortraitHeight(for: presentation.layout)
        return min(natural, max(148, availableHeight * 0.32))
    }
}

struct ThemeEditorBackground: View {
    var body: some View {
        LinearGradient(
            colors: [
                Color(uiColor: .systemGroupedBackground),
                Color.accentColor.opacity(0.08),
                Color(uiColor: .systemGroupedBackground),
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
        .ignoresSafeArea()
    }
}
