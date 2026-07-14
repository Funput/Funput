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

struct AppearanceThemeActionBar: View {
    let isApplied: Bool
    let isCustom: Bool
    let apply: () -> Void
    let customize: () -> Void

    var body: some View {
        if #available(iOS 26, *) {
            GlassEffectContainer(spacing: 12) {
                actions
            }
        } else {
            actions
        }
    }

    private var actions: some View {
        HStack(spacing: 10) {
            if isApplied {
                Label("Đang dùng", systemImage: "checkmark.circle.fill")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .accessibilityIdentifier("appearance.appliedStatus")
            }
            Spacer(minLength: 8)
            customizeButton
            if !isApplied {
                applyButton
            }
        }
        .frame(maxWidth: .infinity)
    }

    @ViewBuilder private var customizeButton: some View {
        let button = Button(action: customize) {
            Label(isCustom ? "Chỉnh sửa" : "Tùy chỉnh", systemImage: isCustom ? "pencil" : "slider.horizontal.3")
        }
        .accessibilityIdentifier("appearance.customize")

        if #available(iOS 26, *) {
            button.buttonStyle(.glass)
        } else {
            button.buttonStyle(.bordered)
        }
    }

    @ViewBuilder private var applyButton: some View {
        let button = Button(action: apply) {
            Label("Áp dụng", systemImage: "paintbrush.fill")
        }
        .accessibilityIdentifier("appearance.apply")

        if #available(iOS 26, *) {
            button.buttonStyle(.glassProminent)
        } else {
            button.buttonStyle(.borderedProminent)
        }
    }
}
