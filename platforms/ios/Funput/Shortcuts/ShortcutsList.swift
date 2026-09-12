#if DEBUG
import FunputShared
import SwiftUI

struct ShortcutsList: View {
    @Bindable var model: ShortcutsModel
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
                    Label("Bản xem trước", systemImage: "eye")
                        .font(.caption.weight(.semibold)).foregroundStyle(.tint)
                    Text("Dữ liệu được lưu trên thiết bị, chưa áp dụng khi gõ.")
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
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                        .accessibilityIdentifier("shortcuts.search")
                    if !model.query.isEmpty {
                        Button("Xoá tìm kiếm", systemImage: "xmark.circle.fill") { model.query = "" }
                            .labelStyle(.iconOnly)
                            .foregroundStyle(.secondary)
                            .frame(minWidth: 44, minHeight: 44)
                    }
                }
                .frame(minHeight: 44)
            }
            Section {
                if !model.hasLoaded || model.loadError != nil {
                    EmptyView()
                } else if model.entries.isEmpty {
                    emptyState
                } else if model.filteredEntries.isEmpty {
                    ContentUnavailableView {
                        Label("Không tìm thấy gõ tắt", systemImage: "magnifyingglass")
                    } description: {
                        Text("Thử tìm bằng chữ tắt hoặc một phần nội dung khác.")
                    } actions: {
                        Button("Xoá tìm kiếm") { model.query = "" }
                            .frame(minHeight: 44)
                    }
                } else {
                    ForEach(model.filteredEntries) { entry in
                        row(entry)
                    }
                }
            } header: {
                Text(!model.hasLoaded || model.loadError != nil ? "Danh sách" : model.query.isEmpty
                     ? "Danh sách · \(model.entries.count) mục"
                     : "Kết quả · \(model.filteredEntries.count)/\(model.entries.count) mục")
                    .textCase(nil)
            }
        }
        .listStyle(.insetGrouped)
        .listSectionSpacing(16)
        .contentMargins(.top, 12, for: .scrollContent)
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
        ContentUnavailableView {
            Label("Chưa có gõ tắt", systemImage: "text.append")
        } description: {
            Text("Lưu những nội dung thường dùng.\nVí dụ: vn → việt nam, kg → không.")
        } actions: {
            Button("Thêm gõ tắt", systemImage: "plus", action: add)
                .disabled(!model.canWrite)
                .buttonStyle(.borderedProminent)
                .frame(minHeight: 44)
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
            .contentShape(.rect)
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
#endif
