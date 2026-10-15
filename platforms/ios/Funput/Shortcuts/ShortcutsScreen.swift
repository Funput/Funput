import FunputShared
import SwiftUI

struct ShortcutsScreen: View {
    @Environment(\.scenePhase) private var scenePhase
    @ObservedObject var model: ShortcutsModel
    @State private var editor: TextShortcut?
    @State private var showsOptions = false

    var body: some View {
        ShortcutsList(model: model, edit: { editor = $0 }, add: { editor = TextShortcut() })
            .navigationTitle(AppTab.shortcuts.title)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { showsOptions = true } label: { Label("Tuỳ chọn", systemImage: "slider.horizontal.3") }
                        .accessibilityIdentifier("shortcuts.options")
                        .disabled(!model.hasLoaded)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { editor = TextShortcut() } label: { Label("Thêm gõ tắt", systemImage: "plus") }
                        .accessibilityIdentifier("shortcuts.add")
                        .disabled(!model.canWrite)
                }
            }
            .sheet(item: $editor) { ShortcutEditor(model: model, original: $0) }
            .sheet(isPresented: $showsOptions) { ShortcutsOptions(model: model) }
            .shortcutsSaveAlert(model, active: editor == nil && !showsOptions)
            .task { await model.reload() }
            .background {
                if #available(iOS 17, *) {
                    Color.clear.onChange(of: scenePhase) { _, phase in reloadIfActive(phase) }
                } else {
                    Color.clear.onChange(of: scenePhase) { phase in reloadIfActive(phase) }
                }
            }
    }

    private func reloadIfActive(_ phase: ScenePhase) {
        if phase == .active { Task { await model.reload() } }
    }
}

#if DEBUG
#Preview("Gõ tắt · Sáng") {
    NavigationStack { ShortcutsScreen(model: ShortcutsModel(store: ShortcutsPreviewStore())) }
        .preferredColorScheme(.light)
}

#Preview("Gõ tắt · Tối") {
    NavigationStack { ShortcutsScreen(model: ShortcutsModel(store: ShortcutsPreviewStore())) }
        .preferredColorScheme(.dark)
}

#Preview("Gõ tắt · Trống") {
    NavigationStack { ShortcutsScreen(model: ShortcutsModel(store: ShortcutsPreviewStore(library: ShortcutLibrary()))) }
}
#endif
