import SwiftUI
import ThemeSchema

struct ThemeEditorPages: View {
    @Binding var selection: ThemeEditorTab
    @Binding var draft: ThemeEditorDraft

    var body: some View {
        TabView(selection: $selection) {
            ForEach(ThemeEditorTab.allCases) { tab in
                ThemeEditorTabPage(tab: tab, draft: $draft)
                    .tag(tab)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
    }
}

private struct ThemeEditorTabPage: View {
    let tab: ThemeEditorTab
    @Binding var draft: ThemeEditorDraft

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                switch tab {
                case .general:
                    ThemeEditorNameCard(name: $draft.customTheme.theme.metadata.name)
                    ThemeGeometryControls(draft: $draft)
                    ThemeResetButton(draft: $draft)
                case .background:
                    ThemeMaterialControls(draft: $draft)
                    ThemeBackgroundControls(draft: $draft)
                    ThemeBackgroundStyleControls(draft: $draft)
                case .keys:
                    ThemeKeyColorControls(draft: $draft)
                    ThemeKeyOpacityControls(draft: $draft)
                    ThemeBorderControls(draft: $draft)
                    ThemeTextColorControls(draft: $draft)
                    ThemeShadowControls(draft: $draft)
                    ThemeContrastWarnings(draft: draft)
                case .pressed:
                    ThemePressedControls(draft: $draft)
                }
            }
            .frame(maxWidth: 720)
            .padding(.horizontal, 18)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
        }
        .accessibilityIdentifier("themeEditor.scroll.\(tab.rawValue)")
        .scrollDismissesKeyboard(.interactively)
    }
}

private struct ThemeResetButton: View {
    @Binding var draft: ThemeEditorDraft

    var body: some View {
        Button("Khôi phục theme gốc", systemImage: "arrow.counterclockwise") {
            draft.resetToBase()
        }
        .buttonStyle(.bordered)
        .frame(maxWidth: .infinity)
        .accessibilityIdentifier("themeEditor.reset")
    }
}
