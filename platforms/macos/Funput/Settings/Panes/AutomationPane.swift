import SwiftUI

struct AutomationPane: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var section = AutomationSection.shortcuts

    var body: some View {
        SettingsPage(destination: .automation) {
            VStack(alignment: .leading, spacing: Theme.Spacing.lg) {
                GlassAutomationSelector(selection: $section)

                Group {
                    switch section {
                    case .shortcuts:
                        ShortcutsPane()
                    case .applications:
                        AppExclusionPane()
                    }
                }
                .transition(reduceMotion ? .identity : .opacity.combined(with: .scale(scale: 0.985)))
            }
        }
    }
}

#Preview {
    AutomationPane()
        .environment(AppSettings.shared)
        .frame(width: 740, height: 680)
}
