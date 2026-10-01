import SwiftUI

/// "Phụ âm đầu mở rộng": one switch like UniKey's, then a chip per letter so the
/// user keeps only the ones they type. Turning the switch on picks all four;
/// clearing the last chip turns it off.
struct ExtraOnsetsSection: View {
    @Environment(AppSettings.self) private var settings
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        @Bindable var settings = settings

        Section("Phụ âm đầu mở rộng") {
            SettingsRow(
                title: "Cho phép z, f, w, j đầu từ",
                subtitle: "Gõ teencode, từ mượn và tên riêng: zô, fải, wá, jờ",
                systemImage: "character.cursor.ibeam"
            ) {
                Toggle("Cho phép z, f, w, j đầu từ", isOn: enabled)
                    .labelsHidden()
                    .toggleStyle(.switch)
                    .tint(Theme.accent)
            }

            if !settings.extraOnsets.isEmpty {
                VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
                    ExtraOnsetChips(selection: $settings.extraOnsets)
                    Text(notes)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.vertical, Theme.Spacing.xs)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .animation(reduceMotion ? nil : .snappy(duration: 0.28), value: settings.extraOnsets.isEmpty)
    }

    /// On picks every letter, the UniKey default; off clears them.
    private var enabled: Binding<Bool> {
        Binding(
            get: { !settings.extraOnsets.isEmpty },
            set: { settings.extraOnsets = $0 ? .all : [] }
        )
    }

    /// What the chosen letters let the user type, then the trade-off every IME's
    /// switch makes, then the Full Telex hint when it applies.
    private var notes: String {
        let examples = ExtraOnsetLetters.letters
            .filter { settings.extraOnsets.contains($0.member) }
            .map(\.examples)
            .joined(separator: " · ")
        var lines = [
            "Gõ được: \(examples).",
            "Từ tiếng Anh có vần tiếng Việt cũng được bỏ dấu (fast → fát) — "
                + "bấm phím lật hoặc gõ đúp phím dấu (fasst) để giữ tiếng Anh.",
        ]
        if settings.inputMethod == .telexAdvanced, settings.extraOnsets.contains(.w) {
            lines.append("Telex nâng cao: w vẫn là ư — gõ ww để có phụ âm w (wwas → wá).")
        }
        return lines.joined(separator: "\n")
    }
}

#Preview {
    Form {
        ExtraOnsetsSection()
    }
    .formStyle(.grouped)
    .environment(AppSettings.shared)
    .frame(width: 760, height: 360)
}
