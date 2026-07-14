import SwiftUI

struct SettingsSelectionSheet: View {
    @Environment(\.dismiss) private var dismiss
    let picker: SettingsPicker
    let model: SettingsModel

    var body: some View {
        NavigationStack {
            ZStack {
                Color(uiColor: .systemGroupedBackground).ignoresSafeArea()
                ScrollView {
                    AdaptiveGlassCard {
                        VStack(spacing: 0) {
                            ForEach(Array(picker.choices(model: model).enumerated()), id: \.element.id) { index, choice in
                                if index > 0 { Divider() }
                                choiceRow(choice)
                            }
                        }
                    }
                    .padding(18)
                }
            }
            .navigationTitle(picker.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Xong") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
    }

    private func choiceRow(_ choice: SettingsChoice) -> some View {
        Button {
            choice.select()
            if !model.showsSaveError { dismiss() }
        } label: {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(choice.title).font(.body.weight(.medium)).foregroundStyle(.primary)
                    Text(choice.summary).font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                if choice.isSelected {
                    Image(systemName: "checkmark.circle.fill").foregroundStyle(.tint)
                }
            }
            .padding(.vertical, 14)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(choice.title)
        .accessibilityValue(choice.isSelected ? "Đang chọn" : "Chưa chọn")
    }
}
