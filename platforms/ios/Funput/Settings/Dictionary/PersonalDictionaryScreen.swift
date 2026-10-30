import PersonalDictionary
import SwiftUI

/// "Từ điển cá nhân": the words the suggestion bar learned, the ones added by hand,
/// and the ones it must never offer.
///
/// A `List` rather than `SettingsPage`, whose non-lazy stack is for short forms; a
/// dictionary holds thousands of words.
struct PersonalDictionaryScreen: View {
    @Environment(\.scenePhase) private var scenePhase
    @ObservedObject var settings: SettingsModel
    @StateObject private var model: PersonalDictionaryModel
    @State private var addsWord = false
    @State private var confirmsReset = false

    init(settings: SettingsModel, store: (any PersonalDictionaryStoring)? = nil) {
        self.settings = settings
        _model = StateObject(wrappedValue: PersonalDictionaryModel(
            store: store ?? PersonalDictionaryStoreFactory.make()
        ))
    }

    var body: some View {
        PersonalDictionaryList(model: model, settings: settings, add: { addsWord = true })
            .navigationTitle(SettingsDestination.personalDictionary.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                // One item, so the two actions sit together rather than the system's
                // spacing apart, and share one glass capsule on iOS 26.
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack(spacing: 4) {
                        resetButton
                        Button { addsWord = true } label: { Label("Thêm từ", systemImage: "plus") }
                            .accessibilityIdentifier("dictionary.add")
                    }
                }
            }
            .sheet(isPresented: $addsWord) { DictionaryAddSheet(model: model) }
            .alert("Không thể cập nhật từ điển", isPresented: actionErrorBinding) {
                Button("Đóng", role: .cancel) {}
            } message: {
                Text(model.actionError ?? "")
            }
            .task { await model.reload() }
            .background {
                if #available(iOS 17, *) {
                    Color.clear.onChange(of: scenePhase) { _, phase in reloadIfActive(phase) }
                } else {
                    Color.clear.onChange(of: scenePhase) { phase in reloadIfActive(phase) }
                }
            }
    }

    /// The wipe, beside "+". The app cannot clear the store itself; the keyboard does
    /// it the next time it opens, so there is nothing to ask for while one waits.
    private var resetButton: some View {
        Button(role: .destructive) { confirmsReset = true } label: {
            Label("Xoá toàn bộ từ đã học", systemImage: "trash")
        }
        .tint(.red)
        .disabled(!canReset)
        .accessibilityIdentifier("dictionary.reset")
        // Anchored to the button: iOS 26 points a confirmation dialog at whatever
        // presents it.
        .confirmationDialog("Xoá toàn bộ từ đã học?", isPresented: $confirmsReset, titleVisibility: .visible) {
            Button("Xoá tất cả", role: .destructive) { settings.requestPersonalSuggestionReset() }
        } message: {
            Text("Xoá cả từ tự thêm khi bạn mở bàn phím lần tới. Từ bị chặn vẫn được giữ.")
        }
    }

    private var canReset: Bool {
        !settings.personalSuggestionResetIsPending
            && model.page.count(of: .learned) + model.page.count(of: .added) > 0
    }

    /// The sheet shows its own errors; this alert is for edits made on the list.
    private var actionErrorBinding: Binding<Bool> {
        Binding(
            get: { model.actionError != nil && !addsWord },
            set: { if !$0 { model.actionError = nil } }
        )
    }

    private func reloadIfActive(_ phase: ScenePhase) {
        if phase == .active { Task { await model.reload() } }
    }
}

#if DEBUG
#Preview("Từ điển cá nhân") {
    NavigationStack {
        PersonalDictionaryScreen(settings: SettingsModel(store: PreviewConfigurationStore()), store: DictionaryPreviewStore())
    }
}

#Preview("Từ điển cá nhân, trống") {
    NavigationStack {
        PersonalDictionaryScreen(settings: SettingsModel(store: PreviewConfigurationStore()), store: DictionaryPreviewStore(words: []))
    }
    .preferredColorScheme(.dark)
}
#endif
