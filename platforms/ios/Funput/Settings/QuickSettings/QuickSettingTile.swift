import SwiftUI

/// One quick setting: icon, short name and state, tinted while on.
///
/// The tap target is a clear button laid over the glass rather than the glass itself,
/// the same arrangement as ``ThemeCard``: an interactive glass surface can take the touch
/// before a button inside it sees it.
struct QuickSettingTile: View {
    let setting: QuickSetting
    let isOn: Bool
    let isLocked: Bool
    let state: String
    let action: () -> Void

    var body: some View {
        ZStack {
            InteractiveGlassCard(isSelected: isOn) {
                VStack(spacing: 4) {
                    Image(systemName: setting.systemImage)
                        .font(.title3)
                        .foregroundStyle(isOn ? AnyShapeStyle(.tint) : AnyShapeStyle(.secondary))
                        .frame(height: 26)
                    HStack(spacing: 4) {
                        Text(setting.title)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(.primary)
                        if setting == .haptics {
                            Image(systemName: "chevron.down")
                                .font(.caption2.weight(.semibold))
                                .foregroundStyle(.secondary)
                        }
                    }
                    Text(state)
                        .font(.caption)
                        .foregroundStyle(isOn ? AnyShapeStyle(.tint) : AnyShapeStyle(.secondary))
                }
                .lineLimit(1)
                .minimumScaleFactor(0.75)
                .frame(maxWidth: .infinity, minHeight: 74)
            }
            .opacity(isLocked ? 0.7 : 1)
            Button(action: action) {
                Color.clear
                    .contentShape(Rectangle())
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .buttonStyle(.plain)
            .disabled(isLocked)
            .accessibilityLabel(setting.accessibilityTitle)
            .accessibilityValue(state)
            .accessibilityHint(accessibilityHint)
            .accessibilityIdentifier("settings.quick.\(setting)")
        }
    }

    private var accessibilityHint: String {
        if isLocked { return "VNI dùng hàng số để bỏ dấu" }
        return setting == .haptics ? "Chạm hai lần để chọn mức rung" : "Chạm hai lần để bật hoặc tắt"
    }
}
