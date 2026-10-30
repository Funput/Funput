import PersonalDictionary
import SwiftUI

/// The page's sections, top to bottom: what needs fixing, the totals and the list
/// switch, the search, and the words under their count and sort.
struct PersonalDictionaryList: View {
    @ObservedObject var model: PersonalDictionaryModel
    @ObservedObject var settings: SettingsModel
    let add: () -> Void
    @State private var pendingForget: DictionaryWord?

    /// While a wipe waits for the keyboard, the learned and added words are as good
    /// as gone; showing them would invite edits to words about to disappear.
    private var hidesWords: Bool {
        settings.personalSuggestionResetIsPending && model.filter != .blocked
    }

    var body: some View {
        List {
            Section { DictionaryNotices(settings: settings) }
            Section { DictionaryHeaderCard(filter: $model.filter, counts: count) }
            if let error = model.loadError {
                Section { loadError(error) }
            }
            Section { DictionarySearchField(text: $model.search) }
            Section {
                words
            } header: {
                if model.hasLoaded, !shown.isEmpty { wordsHeader }
            }
        }
        .listStyle(.insetGrouped)
        .environment(\.defaultMinListRowHeight, 44)
        .modifier(ListSectionSpacing())
        .scrollDismissesKeyboard(.interactively)
        .accessibilityIdentifier("dictionary.list")
        .alert("Xoá “\(pendingForget?.text ?? "")” khỏi từ điển?", isPresented: forgetBinding) {
            Button("Huỷ", role: .cancel) {}
            Button("Xoá", role: .destructive) {
                if let word = pendingForget { Task { await model.forget(word) } }
            }
        } message: {
            Text("Funput quên số lần bạn đã gõ từ này. Gõ lại vài lần thì từ được học lại.")
        }
    }

    private var shown: [DictionaryWord] { hidesWords ? [] : model.page.words }

    private func count(_ filter: DictionaryFilter) -> Int {
        hidesWords && filter != .blocked ? 0 : model.page.count(of: filter)
    }

    /// The count, and the sort beside it. The blocked list keeps the store's own
    /// order, so it has nothing to sort.
    private var wordsHeader: some View {
        HStack {
            Text(header).textCase(nil)
            Spacer(minLength: 8)
            if model.filter != .blocked { DictionarySortMenu(sort: $model.sort) }
        }
    }

    private var header: String {
        let total = count(model.filter)
        return model.search.isEmpty ? "\(total.formatted()) từ"
            : "Tìm thấy \(shown.count.formatted()) trên \(total.formatted()) từ"
    }

    @ViewBuilder private var words: some View {
        if !model.hasLoaded || model.loadError != nil {
            EmptyView()
        } else if shown.isEmpty {
            DictionaryEmptyState(
                filter: model.filter,
                isSearching: !model.search.isEmpty,
                clearSearch: { model.search = "" },
                add: add
            )
        } else {
            ForEach(shown) { word in
                DictionaryWordRow(
                    word: word,
                    highestUses: model.highestUses,
                    block: { Task { await model.block(word) } },
                    unblock: { Task { await model.unblock(word) } },
                    forget: { pendingForget = word }
                )
            }
        }
    }

    private func loadError(_ message: String) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Label("Không mở được từ điển", systemImage: "exclamationmark.triangle").font(.headline)
            Text(message).font(.subheadline).foregroundStyle(.secondary)
            Button("Thử lại") { Task { await model.reload() } }
                .accessibilityIdentifier("dictionary.retry")
        }
        .padding(.vertical, 6)
    }

    private var forgetBinding: Binding<Bool> {
        Binding(get: { pendingForget != nil }, set: { if !$0 { pendingForget = nil } })
    }
}
