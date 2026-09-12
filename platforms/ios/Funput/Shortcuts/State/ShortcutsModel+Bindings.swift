#if DEBUG
import FunputShared
import SwiftUI

extension ShortcutsModel {
    func binding(_ keyPath: WritableKeyPath<ShortcutLibrary, Bool>) -> Binding<Bool> {
        Binding(
            get: { self.library[keyPath: keyPath] },
            set: { value in Task { await self.update(keyPath, to: value) } }
        )
    }

    func saveErrorBinding(active: Bool) -> Binding<Bool> {
        Binding(get: { active && self.saveError != nil }, set: { if !$0 && active { self.saveError = nil } })
    }
}

extension View {
    func shortcutsSaveAlert(_ model: ShortcutsModel, active: Bool = true) -> some View {
        alert("Không thể lưu Gõ tắt", isPresented: model.saveErrorBinding(active: active)) {
            Button("Đóng", role: .cancel) { model.saveError = nil }
        } message: {
            Text(model.saveError ?? "Vui lòng thử lại.")
        }
    }
}
#endif
