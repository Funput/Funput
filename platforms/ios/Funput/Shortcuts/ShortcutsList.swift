import FunputShared
import SwiftUI

struct ShortcutsList: View {
    @ObservedObject var model: ShortcutsModel
    let edit: (TextShortcut) -> Void
    let add: () -> Void
    @State private var pendingDelete: TextShortcut?

    var body: some View {
        List {
            Section {
                ContentCard {
                    SettingsToggleRow(
                        title: "Bật gõ tắt",
                        summary: "Gõ chữ tắt rồi dấu cách hoặc dấu câu để thay bằng nội dung đầy đủ.",
                        isOn: model.binding(\.isEnabled)
                    )
                    .lineLimit(nil)
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityIdentifier("shortcuts.enabled")
                    .disabled(!model.canWrite)
                    Divider()
                    Label("Gõ tắt trên bàn phím Funput", systemImage: "keyboard")
                        .font(.caption.weight(.semibold)).foregroundStyle(.tint)
                    Text("Thay đổi có hiệu lực khi mở lại bàn phím Funput.")
                        .font(.caption).foregroundStyle(.secondary)
                    if model.isSaving { ProgressView("Đang lưu…").font(.caption) }
                }
                .listRowInsets(EdgeInsets())
                .listRowBackground(Color.clear)
            }
            if model.loadError != nil || model.isLoading {
                Section { ShortcutsLoadStatus(model: model) }
            }
            Section {
                HStack(spacing: 10) {
                    Image(systemName: "magnifyingglass").foregroundStyle(.secondary)
                    TextField("Tìm chữ tắt hoặc nội dung", text: $model.query)
                        .frame(minHeight: 44)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                        .accessibilityIdentifier("shortcuts.search")
                    if !model.query.isEmpty {
                        Button { model.query = "" } label: {
                            Label("Xoá tìm kiếm", systemImage: "xmark.circle.fill")
                                .labelStyle(.iconOnly)
                                .foregroundStyle(.secondary)
                                .frame(minWidth: 44, minHeight: 44)
                                .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("shortcuts.search.clear")
                    }
                }
                .frame(minHeight: 44)
                .listRowInsets(EdgeInsets(top: 0, leading: 14, bottom: 0, trailing: 4))
            }
            Section {
                if !model.hasLoaded || model.loadError != nil {
                    EmptyView()
                } else if model.entries.isEmpty {
                    emptyState
                } else if model.filteredEntries.isEmpty {
                    ShortcutsEmptyState(
                        title: "Không tìm thấy gõ tắt", systemImage: "magnifyingglass",
                        summary: "Thử tìm bằng chữ tắt hoặc một phần nội dung khác."
                    ) {
                        Button("Xoá tìm kiếm") { model.query = "" }
                            .frame(minHeight: 44)
                    }
                } else {
                    ForEach(model.filteredEntries) { entry in
                        row(entry)
                    }
                }
            } header: {
                if !model.entries.isEmpty {
                    Text(model.query.isEmpty ? "Danh sách · \(model.entries.count) mục"
                         : "Kết quả · \(model.filteredEntries.count)/\(model.entries.count) mục")
                        .textCase(nil)
                }
            }
        }
        .listStyle(.insetGrouped)
        .environment(\.defaultMinListRowHeight, 44)
        .modifier(ShortcutsListSpacing())
        .scrollDismissesKeyboard(.interactively)
        .accessibilityIdentifier("shortcuts.list")
        .alert("Xoá gõ tắt?", isPresented: Binding(
            get: { pendingDelete != nil },
            set: { if !$0 { pendingDelete = nil } }
        )) {
            Button("Huỷ", role: .cancel) { pendingDelete = nil }
            Button("Xoá", role: .destructive) {
                if let entry = pendingDelete { Task { await model.delete(entry) } }
                pendingDelete = nil
            }
        } message: {
            Text("Bạn muốn xoá “\(pendingDelete?.trigger ?? "")” khỏi danh sách?")
        }
    }

    private var emptyState: some View {
        ShortcutsEmptyState(
            title: "Chưa có gõ tắt", systemImage: "text.append",
            summary: "Gõ ít hơn với những nội dung thường dùng.\nVí dụ: vn → việt nam"
        ) {
            ShortcutsAddButton(action: add)
                .disabled(!model.canWrite)
                .accessibilityIdentifier("shortcuts.empty.add")
        }
    }

    private func row(_ entry: TextShortcut) -> some View {
        Button { edit(entry) } label: {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(entry.trigger).font(.body.weight(.semibold)).foregroundStyle(.primary)
                    Text(entry.expansion).font(.subheadline).foregroundStyle(.secondary).lineLimit(2)
                }
                Spacer(minLength: 0)
                Image(systemName: "chevron.right").font(.caption.bold()).foregroundStyle(.tertiary)
            }
            .frame(minHeight: 44, alignment: .leading)
            .padding(.vertical, 6)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!model.canWrite)
        .accessibilityLabel("\(entry.trigger), \(entry.expansion)")
        .accessibilityHint("Chạm hai lần để sửa gõ tắt")
        .accessibilityIdentifier("shortcuts.entry.\(entry.trigger)")
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            Button("Xoá", role: .destructive) { pendingDelete = entry }
        }
    }
}
