import SwiftUI

struct AppearancePreviewHeader: View {
    @Binding var mode: AppearancePreviewMode

    var body: some View {
        AdaptiveGlassCard {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Label("Xem trước", systemImage: "keyboard")
                        .font(.headline)
                    Text("Chạm thử bàn phím và kiểm tra theme ở cả hai chế độ.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer(minLength: 12)
            }
            Picker("Chế độ xem trước", selection: $mode) {
                ForEach(AppearancePreviewMode.allCases) { mode in
                    Text(mode.title).tag(mode)
                }
            }
            .pickerStyle(.segmented)
            .accessibilityIdentifier("appearance.previewMode")
        }
    }
}

struct AppearanceApplyButton: View {
    let isApplied: Bool
    let action: () -> Void

    var body: some View {
        if #available(iOS 26, *) {
            button.buttonStyle(.glassProminent)
        } else {
            button.buttonStyle(.borderedProminent)
        }
    }

    private var button: some View {
        Button(action: action) {
            Label(isApplied ? "Đang sử dụng" : "Áp dụng theme", systemImage: isApplied ? "checkmark" : "paintbrush.fill")
                .frame(maxWidth: .infinity, minHeight: 32)
        }
        .disabled(isApplied)
        .accessibilityIdentifier("appearance.apply")
    }
}

struct AppearanceCustomizeButton: View {
    let isCustom: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Label(
                isCustom ? "Chỉnh sửa theme" : "Tạo bản tùy chỉnh",
                systemImage: isCustom ? "slider.horizontal.3" : "plus.square.on.square"
            )
            .frame(maxWidth: .infinity, minHeight: 32)
        }
        .buttonStyle(.bordered)
        .accessibilityIdentifier("appearance.customize")
    }
}
