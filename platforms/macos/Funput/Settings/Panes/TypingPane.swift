import SwiftUI

struct TypingPane: View {
    @Environment(AppSettings.self) private var settings

    var body: some View {
        @Bindable var settings = settings

        SettingsPage(destination: .typing) {
            Section("Cách đặt dấu") {
                Picker("Kiểu đặt dấu", selection: $settings.toneStyle) {
                    ForEach(ToneStyle.allCases) { style in
                        Text(style.displayName).tag(style)
                    }
                }
                .pickerStyle(.segmented)

                Text(settings.toneStyle.blurb)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Section("Xử lý thông minh") {
                SettingsRow(
                    title: "Tự khôi phục từ tiếng Anh",
                    subtitle: "Giữ nguyên từ không phải tiếng Việt",
                    systemImage: "wand.and.stars"
                ) {
                    Toggle("Tự khôi phục từ tiếng Anh", isOn: $settings.smartEnglishRestore)
                        .labelsHidden()
                        .toggleStyle(.switch)
                        .tint(Theme.accent)
                }

                SettingsRow(
                    title: "Khôi phục tức thì",
                    subtitle: "Không chờ dấu cách khi đã nhận ra từ tiếng Anh",
                    systemImage: "bolt"
                ) {
                    Toggle("Khôi phục tức thì", isOn: $settings.eagerRestore)
                        .labelsHidden()
                        .toggleStyle(.switch)
                        .tint(Theme.accent)
                        .disabled(!settings.smartEnglishRestore)
                }

                SettingsRow(
                    title: "Kiểm tra chính tả",
                    subtitle: "Chỉ đặt dấu khi tạo thành âm tiết hợp lệ",
                    systemImage: "checkmark.seal"
                ) {
                    Toggle("Kiểm tra chính tả", isOn: $settings.spellCheckEnabled)
                        .labelsHidden()
                        .toggleStyle(.switch)
                        .tint(Theme.accent)
                }

                SettingsRow(
                    title: "Tự động viết hoa",
                    subtitle: "Viết hoa đầu câu và sau dấu kết thúc",
                    systemImage: "textformat.abc.dottedunderline"
                ) {
                    Toggle("Tự động viết hoa", isOn: $settings.autoCapitalizeEnabled)
                        .labelsHidden()
                        .toggleStyle(.switch)
                        .tint(Theme.accent)
                }
            }
        }
    }
}

#Preview {
    TypingPane()
        .environment(AppSettings.shared)
        .frame(width: 760, height: 760)
}
