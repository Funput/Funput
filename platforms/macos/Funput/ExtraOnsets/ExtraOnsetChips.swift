import SwiftUI

/// One Liquid Glass chip per extra onset letter; each toggles on its own, so any
/// combination can be chosen. Same glass treatment as `GlassMethodSelector`: a
/// selected chip takes the accent tint, every chip is interactive glass.
struct ExtraOnsetChips: View {
    @Binding var selection: ExtraOnsetLetters
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        GlassEffectContainer(spacing: Theme.Spacing.sm) {
            HStack(spacing: Theme.Spacing.sm) {
                ForEach(ExtraOnsetLetters.letters) { letter in
                    chip(letter, isOn: selection.contains(letter.member))
                }
            }
        }
        .animation(reduceMotion ? nil : .snappy(duration: 0.28), value: selection)
    }

    private func chip(_ letter: ExtraOnsetLetters.Letter, isOn: Bool) -> some View {
        Button {
            toggle(letter.member)
        } label: {
            VStack(spacing: 2) {
                Text(letter.symbol)
                    .font(.system(.title3, design: .rounded).weight(.semibold))
                Text(letter.examples)
                    .font(.caption2)
                    .foregroundStyle(isOn ? AnyShapeStyle(.white.opacity(0.85)) : AnyShapeStyle(.secondary))
                    .lineLimit(1)
            }
            .padding(.vertical, Theme.Spacing.sm)
            .frame(maxWidth: .infinity)
            .contentShape(.capsule)
        }
        .buttonStyle(.plain)
        .foregroundStyle(isOn ? .white : .primary)
        .glassEffect(.regular.tint(isOn ? Theme.accent : nil).interactive(), in: .capsule)
        .help("Phụ âm đầu \(letter.symbol): \(letter.examples)")
        .accessibilityLabel("Phụ âm đầu \(letter.symbol)")
        .accessibilityValue(isOn ? "Bật" : "Tắt")
        .accessibilityAddTraits(isOn ? .isSelected : [])
    }

    private func toggle(_ member: ExtraOnsetLetters) {
        if selection.contains(member) {
            selection.remove(member)
        } else {
            selection.insert(member)
        }
    }
}

#Preview("Extra onset chips") {
    @Previewable @State var letters: ExtraOnsetLetters = [.z, .j]
    ExtraOnsetChips(selection: $letters)
        .padding(40)
        .frame(width: 520)
}
