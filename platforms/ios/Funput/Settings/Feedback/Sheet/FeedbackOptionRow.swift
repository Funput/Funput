import SwiftUI

/// One row of a single-choice feedback sheet: a symbol, a title and summary, and a check.
struct FeedbackOptionRow<Symbol: View>: View {
    let title: String
    let summary: String
    let selected: Bool
    let hint: String
    let identifier: String
    let select: () -> Void
    @ViewBuilder let symbol: () -> Symbol

    var body: some View {
        Button(action: select) {
            HStack(spacing: 14) {
                symbol()
                    .frame(width: 26, height: 26)
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
        .accessibilityHint(hint)
        .accessibilityIdentifier(identifier)
    }

    // An accent wash rather than a neutral one: grey on the sheet's glass, or on the
    // grey fallback card, barely separates the selected row from the others.
    private var selectionBackground: some View {
        RoundedRectangle(cornerRadius: 18, style: .continuous)
            .fill(Color.accentColor.opacity(0.16))
    }
}

/// Stacks feedback rows with inset dividers on the sheet's surface.
struct FeedbackOptionStack<Option, Row: View>: View {
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    let options: [Option]
    @ViewBuilder let row: (Option) -> Row

    var body: some View {
        VStack(spacing: 0) {
            ForEach(Array(options.enumerated()), id: \.offset) { index, option in
                if index > 0 {
                    Divider().padding(.leading, 58).padding(.trailing, 14)
                }
                row(option)
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
}
