import SwiftUI

/// The search row, drawn like the one on the shortcuts page. Marks and case do not
/// matter: `duoc` finds `được`.
struct DictionarySearchField: View {
    @Binding var text: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass").foregroundStyle(.secondary).accessibilityHidden(true)
            TextField("Tìm từ", text: $text)
                .frame(minHeight: 44)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
                .accessibilityIdentifier("dictionary.search")
            if !text.isEmpty {
                Button { text = "" } label: {
                    Label("Xoá tìm kiếm", systemImage: "xmark.circle.fill")
                        .labelStyle(.iconOnly)
                        .foregroundStyle(.secondary)
                        .frame(minWidth: 44, minHeight: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("dictionary.search.clear")
            }
        }
        .frame(minHeight: 44)
        .listRowInsets(EdgeInsets(top: 0, leading: 14, bottom: 0, trailing: 4))
    }
}
