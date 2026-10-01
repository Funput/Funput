import SwiftUI

/// A native checkbox per extra onset letter, two to a row. A checkbox says "this can
/// be turned on and off" without explanation, which a tinted chip did not; the
/// letter sits on a `KeyCap` so it reads as the key the user types.
struct ExtraOnsetCheckboxes: View {
    @Binding var selection: ExtraOnsetLetters

    private let columns = [
        GridItem(.flexible(), alignment: .leading),
        GridItem(.flexible(), alignment: .leading),
    ]

    var body: some View {
        LazyVGrid(columns: columns, alignment: .leading, spacing: Theme.Spacing.sm) {
            ForEach(ExtraOnsetLetters.letters) { letter in
                Toggle(isOn: binding(for: letter.member)) {
                    HStack(spacing: Theme.Spacing.sm) {
                        KeyCap(label: letter.symbol)
                        Text(letter.examples)
                            .foregroundStyle(.secondary)
                    }
                }
                .toggleStyle(.checkbox)
                .help("Cho phép phụ âm đầu \(letter.symbol): \(letter.examples)")
                .accessibilityLabel("Phụ âm đầu \(letter.symbol), ví dụ \(letter.examples)")
            }
        }
    }

    private func binding(for member: ExtraOnsetLetters) -> Binding<Bool> {
        Binding(
            get: { selection.contains(member) },
            set: { isOn in
                if isOn {
                    selection.insert(member)
                } else {
                    selection.remove(member)
                }
            }
        )
    }
}

#Preview("Extra onset checkboxes") {
    @Previewable @State var letters: ExtraOnsetLetters = [.z, .f, .j]
    ExtraOnsetCheckboxes(selection: $letters)
        .padding(40)
        .frame(width: 520)
}
