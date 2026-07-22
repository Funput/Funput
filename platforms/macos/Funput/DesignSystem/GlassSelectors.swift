import SwiftUI

struct GlassMethodSelector: View {
    @Binding var selection: InputMethod
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var compact = false

    // Tighter spacing/padding for the menu bar panel so the chips read as
    // slim pills instead of chunky buttons; the non-compact (onboarding)
    // sizing is unchanged.
    private var chipSpacing: CGFloat { compact ? Theme.Spacing.xs : Theme.Spacing.sm }

    var body: some View {
        GlassEffectContainer(spacing: chipSpacing) {
            HStack(spacing: chipSpacing) {
                ForEach(InputMethod.displayCases) { method in
                    Button {
                        select(method)
                    } label: {
                        Text(compact ? compactTitle(for: method) : method.displayName)
                            .font(compact ? .caption.weight(.semibold) : .callout.weight(.semibold))
                            .padding(.horizontal, compact ? Theme.Spacing.sm : Theme.Spacing.md)
                            .padding(.vertical, compact ? 4 : 8)
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

#Preview("Method selector") {
    @Previewable @State var method = InputMethod.telex
    GlassMethodSelector(selection: $method)
        .padding(40)
        .background(VietnameseFlowBackground())
        .frame(width: 520, height: 180)
}

/// Two-way EN/VI toggle that mimics a native `Picker(.segmented)` look
/// (single joined track, sliding highlight) but stays fully custom-drawn.
/// Native `NSSegmentedControl` only paints its selected-segment highlight
/// with the app tint while the window is key/active (AppKit dims it to
/// neutral gray otherwise), so it can't reliably show `Theme.accent` for the
/// active state. A single glass track avoids nested/decorative glass (per
/// the Liquid Glass guidelines) — only the outer capsule is glass, while the
/// selection knob is a plain tinted capsule that slides via
/// `matchedGeometryEffect`, same as the native control's behavior.
struct GlassLanguageToggle: View {
    @Binding var isVietnameseEnabled: Bool
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Namespace private var indicator

    private let segmentSize = CGSize(width: 28, height: 18)

    var body: some View {
        HStack(spacing: 0) {
            segment(title: "EN", isSelected: !isVietnameseEnabled) {
                isVietnameseEnabled = false
            }
            segment(title: "VI", isSelected: isVietnameseEnabled) {
                isVietnameseEnabled = true
            }
        }
        .padding(3)
        .glassEffect(.regular.interactive(), in: .capsule)
        .animation(reduceMotion ? nil : .snappy(duration: 0.28), value: isVietnameseEnabled)
    }

    private func segment(title: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.caption.weight(.semibold))
                .frame(width: segmentSize.width, height: segmentSize.height)
        }
        .buttonStyle(.plain)
        .foregroundStyle(isSelected ? .white : .primary)
        .background {
            if isSelected {
                Capsule()
                    .fill(Theme.accent)
                    .matchedGeometryEffect(id: "selectedSegment", in: indicator)
            }
        }
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .accessibilityValue(isSelected ? "Đã chọn" : "Chưa chọn")
    }
}

#Preview("Language toggle") {
    @Previewable @State var isVietnamese = true
    GlassLanguageToggle(isVietnameseEnabled: $isVietnamese)
        .padding(40)
        .background(VietnameseFlowBackground())
        .frame(width: 220, height: 120)
}
