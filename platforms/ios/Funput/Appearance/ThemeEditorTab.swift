import SwiftUI

enum ThemeEditorTab: String, CaseIterable, Identifiable {
    case general
    case background
    case keys
    case pressed

    var id: String { rawValue }

    var title: String {
        switch self {
        case .general: "Chung"
        case .background: "Nền"
        case .keys: "Phím & chữ"
        case .pressed: "Khi nhấn"
        }
    }
}

struct ThemeEditorTabBar: View {
    @Binding var selection: ThemeEditorTab

    var body: some View {
        Picker("Nhóm cấu hình", selection: $selection) {
            ForEach(ThemeEditorTab.allCases) { tab in
                Text(tab.title)
                    .tag(tab)
                    .accessibilityIdentifier("themeEditor.tab.\(tab.rawValue)")
            }
        }
        .pickerStyle(.segmented)
        .accessibilityIdentifier("themeEditor.tabs")
        .padding(.horizontal, 18)
        .padding(.vertical, 10)
        .background(.bar)
    }
}
