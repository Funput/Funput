import FunputShared
import SwiftUI

/// A compact, single-choice list for the quick-settings sheet.
struct HapticFeedbackList: View {
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    let selection: HapticFeedbackStrength?
    let select: (HapticFeedbackStrength?) -> Void
    private let options: [HapticFeedbackStrength?] = [nil, .light, .medium, .strong]

    var body: some View {
        VStack(spacing: 0) {
            ForEach(Array(options.enumerated()), id: \.offset) { index, strength in
                if index > 0 {
                    Divider().padding(.leading, 58).padding(.trailing, 14)
                }
                row(strength)
            }
        }
        .padding(6)
        .background {
            if !HapticFeedbackSurface.isGlass(reduceTransparency: reduceTransparency) {
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(Color(uiColor: .secondarySystemGroupedBackground))
            }
        }
    }

    private func row(_ strength: HapticFeedbackStrength?) -> some View {
        let selected = selection == strength
        let title = strength?.settingsTitle ?? "Tắt"
        let summary = strength?.settingsSummary ?? "Không rung khi thao tác"
        return Button { select(strength) } label: {
            HStack(spacing: 14) {
                intensitySymbol(strength, selected: selected)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(.body.weight(selected ? .semibold : .medium))
                        .foregroundStyle(selected ? Color.accentColor : Color.primary)
                    Text(summary)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .fixedSize(horizontal: false, vertical: true)
                Spacer(minLength: 8)
                Image(systemName: "checkmark")
                    .font(.body.weight(.semibold))
                    .foregroundStyle(Color.accentColor)
                    .opacity(selected ? 1 : 0)
                    .accessibilityHidden(true)
            }
            .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .contentShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .background {
                if selected { selectionBackground.allowsHitTesting(false) }
            }
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(title), \(summary)")
        .accessibilityValue(selected ? "Đang chọn" : "Chưa chọn")
        .accessibilityAddTraits(selected ? .isSelected : [])
        .accessibilityHint(strength == nil ? "Tắt toàn bộ rung của bàn phím" : "Chọn và cảm nhận mức rung")
        .accessibilityIdentifier("settings.haptics.\(strength?.rawValue ?? "off")")
    }

    private func intensitySymbol(_ strength: HapticFeedbackStrength?, selected: Bool) -> some View {
        let color = selected ? Color.accentColor : Color.secondary
        let level = switch strength {
        case nil: 0
        case .light: 1
        case .medium: 2
        case .strong: 3
        }
        return HStack(alignment: .center, spacing: 3) {
            if level == 0 {
                Image(systemName: "minus").font(.title3.weight(.medium))
            } else {
                ForEach(0..<3) { index in
                    Capsule()
                        .fill(color)
                        .opacity(index < level ? 1 : 0.22)
                        .frame(width: 4, height: CGFloat(10 + index * 6))
                }
            }
        }
        .foregroundStyle(color)
        .frame(width: 26, height: 26)
    }

    // An accent wash rather than a neutral one: grey on the sheet's glass, or on the
    // grey fallback card, barely separates the selected row from the others.
    private var selectionBackground: some View {
        RoundedRectangle(cornerRadius: 18, style: .continuous)
            .fill(Color.accentColor.opacity(0.16))
    }
}
