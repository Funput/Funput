import SwiftUI

/// The second axis of the Chuyển mã window: five transforms, the order they were
/// pressed in, and the two switches that belong to them.
///
/// Not a tab. Changing a charset and changing the case are two choices about one
/// document and they compose, so both stay on screen at once; a tab would say the
/// user has to pick one.
///
/// Glass is for the controls only — the chips, Hoàn tác, Bỏ hết. The `Đang áp:`
/// line is content-layer text and the switches are system controls, so nothing here
/// nests one glass surface inside another (`docs/LIQUID_GLASS.md`).
struct ConvertCasingBar: View {
    let state: ConvertScreenState
    let dispatch: ConvertDispatch
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            chips
            if !state.appliedTransforms.isEmpty {
                applied
            }
        }
        .animation(reduceMotion ? nil : .snappy(duration: 0.28), value: state.appliedTransforms)
    }

    private var chips: some View {
        GlassEffectContainer(spacing: Theme.Spacing.sm) {
            HStack(spacing: Theme.Spacing.sm) {
                Text("Kiểu chữ").font(.callout).foregroundStyle(.secondary).fixedSize()
                ForEach(state.transforms) { transform in
                    chip(transform)
                }
                Spacer()
            }
        }
        .disabled(!state.canUseCasing)
    }

    /// A press appends; pressing the one already on top is a no-op in core, so the
    /// chip is a button rather than a checkbox. The checkmark is the non-color cue
    /// the guidelines require — the tint alone would not be one.
    private func chip(_ transform: ConvertTransform) -> some View {
        let isApplied = state.appliedTransforms.contains(transform.id)
        return Button {
            dispatch(.casing(.apply(transform.id)))
        } label: {
            HStack(spacing: Theme.Spacing.xs) {
                if isApplied {
                    Image(systemName: "checkmark").font(.caption2.weight(.bold))
                }
                Text(transform.name).font(.caption.weight(.semibold))
            }
            .lineLimit(1)
            .padding(.horizontal, Theme.Spacing.sm)
            .padding(.vertical, 5)
        }
        .buttonStyle(.plain)
        .foregroundStyle(isApplied ? .white : .primary)
        .glassEffect(
            .regular.tint(isApplied ? Theme.accent : nil).interactive(),
            in: .capsule
        )
        .accessibilityIdentifier("convert.casing.\(transform.id)")
        .accessibilityAddTraits(isApplied ? .isSelected : [])
        .accessibilityValue(isApplied ? "Đã áp" : "Chưa áp")
    }

    /// The order is the whole point — `chữ thường → Viết Hoa Đầu Mỗi Từ` is a
    /// different document from the same two reversed — so it is spelled out rather
    /// than left for the user to infer from which chips are lit.
    private var applied: some View {
        HStack(spacing: Theme.Spacing.md) {
            Text("Đang áp: \(state.appliedTransformNames.joined(separator: " → "))")
                .font(.callout)
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .accessibilityIdentifier("convert.casing.applied")
            switches
            Spacer()
            GlassEffectContainer(spacing: Theme.Spacing.sm) {
                HStack(spacing: Theme.Spacing.sm) {
                    Button("Hoàn tác", systemImage: "arrow.uturn.backward") {
                        dispatch(.casing(.undo))
                    }
                    .buttonStyle(.glass)
                    .accessibilityIdentifier("convert.casing.undo")
                    Button("Bỏ hết") { dispatch(.casing(.clear)) }
                        .buttonStyle(.glass)
                        .accessibilityIdentifier("convert.casing.clear")
                }
            }
        }
        .transition(.opacity)
    }

    /// A switch appears only while the transform it belongs to is applied: it has
    /// nothing to say otherwise, and showing it there teaches which one owns it.
    @ViewBuilder private var switches: some View {
        if state.showsSwitch(for: .noDiacritics) {
            Toggle("Giữ đ/Đ", isOn: binding(\.keepD) { .setKeepD($0) })
                .toggleStyle(.switch)
                .controlSize(.small)
                .accessibilityIdentifier("convert.casing.keepD")
        }
        if state.showsSwitch(for: .title) {
            Toggle("Hạ chữ viết hoa", isOn: binding(\.flattenCaps) { .setFlattenCaps($0) })
                .toggleStyle(.switch)
                .controlSize(.small)
                .accessibilityIdentifier("convert.casing.flattenCaps")
        }
    }

    private func binding(
        _ value: KeyPath<ConvertScreenState, Bool>,
        _ action: @escaping (Bool) -> ConvertCasingAction
    ) -> Binding<Bool> {
        Binding(get: { state[keyPath: value] }, set: { dispatch(.casing(action($0))) })
    }
}

#Preview("Đã áp hai phép") {
    ConvertCasingBar(state: ConvertFixtures.cased, dispatch: { _ in })
        .padding(Theme.Spacing.xl)
        .frame(width: 920)
        .background(.windowBackground)
}
