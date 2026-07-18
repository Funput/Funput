import SwiftUI

struct SettingsSelectionRow: View {
    let option: SettingsPicker
    let value: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: option.systemImage)
                    .foregroundStyle(.tint)
                    .frame(width: 22)
                VStack(alignment: .leading, spacing: 3) {
                    Text(option.title).foregroundStyle(.primary)
                    Text(option.summary).font(.caption).foregroundStyle(.secondary)
                }
                Spacer(minLength: 12)
                Text(value).font(.subheadline).foregroundStyle(.secondary)
                Image(systemName: "chevron.right").font(.caption.bold()).foregroundStyle(.tertiary)
            }
            .padding(.vertical, 12)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(option.title), \(value)")
        .accessibilityHint("Chạm hai lần để thay đổi")
    }
}

struct SettingsToggleRow: View {
    let title: String
    let summary: String
    @Binding var isOn: Bool

    var body: some View {
        Toggle(isOn: $isOn) {
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                Text(summary).font(.caption).foregroundStyle(.secondary)
            }
        }
        .tint(.accentColor)
        .padding(.vertical, 11)
        .accessibilityHint(summary)
    }
}

struct SettingsHeightRow: View {
    @Binding var value: Double

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Label("Chiều cao bàn phím", systemImage: "arrow.up.and.down")
                Spacer()
                Text(value, format: .percent.precision(.fractionLength(0)))
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
            }
            Slider(value: $value, in: 0.85...1.2, step: 0.01) {
                Text("Chiều cao bàn phím")
            } minimumValueLabel: {
                Text("85%").font(.caption2)
            } maximumValueLabel: {
                Text("120%").font(.caption2)
            }
            .tint(.accentColor)
        }
        .padding(.vertical, 12)
        .accessibilityElement(children: .contain)
    }
}
