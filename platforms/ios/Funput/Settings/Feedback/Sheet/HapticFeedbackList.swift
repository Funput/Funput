import FunputShared
import SwiftUI

/// A compact, single-choice list for the quick-settings sheet.
struct HapticFeedbackList: View {
    let selection: HapticFeedbackStrength?
    let select: (HapticFeedbackStrength?) -> Void
    private let options: [HapticFeedbackStrength?] = [nil, .light, .medium, .strong]

    var body: some View {
        FeedbackOptionStack(options: options) { strength in
            let selected = selection == strength
            FeedbackOptionRow(
                title: strength?.settingsTitle ?? "Tắt",
                summary: strength?.settingsSummary ?? "Không rung khi thao tác",
                selected: selected,
                hint: strength == nil ? "Tắt toàn bộ rung của bàn phím" : "Chọn và cảm nhận mức rung",
                identifier: "settings.haptics.\(strength?.rawValue ?? "off")",
                select: { select(strength) }
            ) {
                intensitySymbol(strength, selected: selected)
            }
        }
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
    }
}
