import SwiftUI

struct GlassMethodSelector: View {
    @Binding var selection: InputMethod
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var compact = false

    var body: some View {
        GlassEffectContainer(spacing: Theme.Spacing.sm) {
            HStack(spacing: Theme.Spacing.sm) {
                ForEach(InputMethod.displayCases) { method in
                    Button {
                        select(method)
                    } label: {
                        HStack(spacing: Theme.Spacing.xs) {
                            Text(compact ? compactTitle(for: method) : method.displayName)
                            if method == selection {
                                Image(systemName: "checkmark")
                                    .font(.caption.bold())
                            }
                        }
                            .font(.callout.weight(.semibold))
                            .padding(.horizontal, compact ? 10 : Theme.Spacing.md)
                            .padding(.vertical, compact ? 6 : 8)
                            .frame(maxWidth: compact ? nil : .infinity)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(method == selection ? .white : .primary)
                    .glassEffect(
                        .regular
                            .tint(method == selection ? Theme.accent : nil)
                            .interactive(),
                        in: .capsule
                    )
                    .accessibilityAddTraits(method == selection ? .isSelected : [])
                    .accessibilityValue(method == selection ? "Đã chọn" : "Chưa chọn")
                }
            }
        }
        .animation(reduceMotion ? nil : .snappy(duration: 0.28), value: selection)
    }

    private func select(_ method: InputMethod) {
        selection = method
    }

    private func compactTitle(for method: InputMethod) -> String {
        switch method {
        case .telex: "Telex"
        case .vni: "VNI"
        case .telexAdvanced: "Telex+"
        }
    }
}

enum AutomationSection: String, CaseIterable, Identifiable {
    case shortcuts
    case applications

    var id: String { rawValue }

    var title: String {
        switch self {
        case .shortcuts: "Gõ tắt"
        case .applications: "Ứng dụng"
        }
    }

    var systemImage: String {
        switch self {
        case .shortcuts: "text.append"
        case .applications: "app.badge"
        }
    }
}

struct GlassAutomationSelector: View {
    @Binding var selection: AutomationSection
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        GlassEffectContainer(spacing: Theme.Spacing.sm) {
            HStack(spacing: Theme.Spacing.sm) {
                ForEach(AutomationSection.allCases) { section in
                    Button {
                        select(section)
                    } label: {
                        HStack(spacing: Theme.Spacing.sm) {
                            Label(section.title, systemImage: section.systemImage)
                            if section == selection {
                                Image(systemName: "checkmark")
                                    .font(.caption.bold())
                            }
                        }
                            .font(.callout.weight(.semibold))
                            .padding(.horizontal, Theme.Spacing.md)
                            .padding(.vertical, 8)
                            .frame(minWidth: 128)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(section == selection ? .white : .primary)
                    .glassEffect(
                        .regular
                            .tint(section == selection ? Theme.accent : nil)
                            .interactive(),
                        in: .capsule
                    )
                    .accessibilityAddTraits(section == selection ? .isSelected : [])
                    .accessibilityValue(section == selection ? "Đã chọn" : "Chưa chọn")
                }
            }
        }
        .animation(reduceMotion ? nil : .snappy(duration: 0.28), value: selection)
    }

    private func select(_ section: AutomationSection) {
        selection = section
    }
}

#Preview("Method selector") {
    @Previewable @State var method = InputMethod.telex
    GlassMethodSelector(selection: $method)
        .padding(40)
        .background(VietnameseFlowBackground())
        .frame(width: 520, height: 180)
}
