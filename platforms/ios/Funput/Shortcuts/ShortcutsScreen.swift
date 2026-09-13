import FunputShared
import SwiftUI

struct ShortcutsScreen: View {
    @Environment(\.scenePhase) private var scenePhase
    @Bindable var model: ShortcutsModel
    @State private var editor: TextShortcut?
    @State private var showsOptions = false

    var body: some View {
        ShortcutsList(model: model, edit: { editor = $0 }, add: { editor = TextShortcut() })
            .navigationTitle("Gõ tắt")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Tuỳ chọn", systemImage: "slider.horizontal.3") { showsOptions = true }
                        .accessibilityIdentifier("shortcuts.options")
                        .disabled(!model.hasLoaded)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Thêm gõ tắt", systemImage: "plus") { editor = TextShortcut() }
                        .accessibilityIdentifier("shortcuts.add")
                        .disabled(!model.canWrite)
                }
            }
            .sheet(item: $editor) { ShortcutEditor(model: model, original: $0) }
            .sheet(isPresented: $showsOptions) { ShortcutsOptions(model: model) }
            .shortcutsSaveAlert(model, active: editor == nil && !showsOptions)
            .task { await model.reload() }
            .onChange(of: scenePhase) { _, phase in
                if phase == .active { Task { await model.reload() } }
            }
    }
}

struct ShortcutsSettingsLink: View {
    let model: ShortcutsModel

    var body: some View {
        NavigationLink {
            ShortcutsScreen(model: model)
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "text.append").foregroundStyle(.tint).frame(width: 22)
                VStack(alignment: .leading, spacing: 3) {
                    Text("Gõ tắt").foregroundStyle(.primary)
                    Text("Thay chữ tắt bằng nội dung dài hơn.")
                        .font(.caption).foregroundStyle(.secondary)
                }
                Spacer(minLength: 0)
                Image(systemName: "chevron.right").font(.caption.bold()).foregroundStyle(.tertiary)
            }
            .frame(minHeight: 44)
            .padding(.vertical, 10)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("settings.shortcuts")
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
