import SwiftUI

struct InteractiveGlassCard<Content: View>: View {
    let isSelected: Bool
    let content: Content

    init(isSelected: Bool, @ViewBuilder content: () -> Content) {
        self.isSelected = isSelected
        self.content = content()
    }

    var body: some View {
        if #available(iOS 26, *) {
            content
                .padding(12)
                .glassEffect(glass, in: .rect(cornerRadius: 20))
        } else {
            content
                .padding(12)
                .background(
                    isSelected ? Color.accentColor.opacity(0.12) : .clear,
                    in: RoundedRectangle(cornerRadius: 20, style: .continuous)
                )
                .background(
                    .regularMaterial,
                    in: RoundedRectangle(cornerRadius: 20, style: .continuous)
                )
        }
    }

    @available(iOS 26, *)
    private var glass: Glass {
        if isSelected {
            return .regular.tint(Color.accentColor.opacity(0.16)).interactive()
        }
        return .regular.interactive()
    }
}
