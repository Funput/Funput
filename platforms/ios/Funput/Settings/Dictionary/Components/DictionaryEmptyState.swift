import PersonalDictionary
import SwiftUI

/// What an empty list says: an invitation for each of the three lists, and a way out
/// of a search that matched nothing.
struct DictionaryEmptyState: View {
    let filter: DictionaryFilter
    let isSearching: Bool
    let clearSearch: () -> Void
    let add: () -> Void

    var body: some View {
        if isSearching {
            EmptyStateView(
                title: "Không tìm thấy từ",
                systemImage: "magnifyingglass",
                summary: "Thử gõ một phần khác của từ. Có dấu hay không dấu đều được."
            ) {
                Button("Xoá tìm kiếm", action: clearSearch).frame(minHeight: 44)
            }
            .accessibilityIdentifier("dictionary.empty.search")
        } else {
            switch filter {
            case .learned:
                EmptyStateView(
                    title: "Chưa có từ nào",
                    systemImage: "text.book.closed",
                    summary: "Funput học một từ sau khi bạn gõ nó vài lần bằng bàn phím Funput."
                ) { EmptyView() }
                .accessibilityIdentifier("dictionary.empty.learned")
            case .added:
                EmptyStateView(
                    title: "Chưa có từ tự thêm",
                    systemImage: "plus.bubble",
                    summary: "Thêm tên riêng, thương hiệu hay biệt danh để Funput gợi ý ngay từ lần đầu."
                ) {
                    GlassAddButton(title: "Thêm từ", action: add)
                        .accessibilityIdentifier("dictionary.empty.add")
                }
                .accessibilityIdentifier("dictionary.empty.added")
            case .blocked:
                EmptyStateView(
                    title: "Chưa chặn từ nào",
                    systemImage: "nosign",
                    summary: "Từ bạn chặn sẽ không bao giờ được gợi ý, kể cả khi bạn gõ lại nó."
                ) { EmptyView() }
                .accessibilityIdentifier("dictionary.empty.blocked")
            }
        }
    }
}
