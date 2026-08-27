import SwiftUI

struct ConvertWindow: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var session: ConvertPrototypeSession

    init(initialState: ConvertScreenState = ConvertFixtures.empty) {
        _session = State(initialValue: ConvertPrototypeSession(state: initialState))
    }

    var body: some View {
        ZStack {
            Color(nsColor: .windowBackgroundColor)
            VietnameseFlowBackground(animated: false)
                .opacity(session.state.mode == .empty ? 0.12 : 0)

            VStack(spacing: Theme.Spacing.lg) {
                ConvertHeader(
                    showsRestart: session.state.mode != .empty,
                    restart: restart
                )
                content
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .padding(Theme.Spacing.xl)
        }
        .tint(Theme.accent)
        .frame(minWidth: 760, minHeight: 520)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.22), value: session.state.mode)
    }

    @ViewBuilder private var content: some View {
        switch session.state.mode {
        case .empty:
            ConvertEmptyView(dispatch: dispatch)
        case .text:
            ConvertTextView(state: session.state, dispatch: dispatch)
        case .files:
            ConvertBatchView(state: session.state, dispatch: dispatch)
        }
    }

    private func dispatch(_ action: ConvertAction) {
        session.send(action)
    }

    private func restart() {
        dispatch(.restart)
    }
}

#Preview("Cửa sổ — Trống") {
    ConvertWindow()
        .frame(width: 920, height: 620)
}

#Preview("Cửa sổ — Văn bản") {
    ConvertWindow(initialState: ConvertFixtures.pasted)
        .frame(width: 920, height: 620)
}

#Preview("Cửa sổ — Một tệp") {
    ConvertWindow(initialState: ConvertFixtures.singleFile)
        .frame(width: 920, height: 620)
}

#Preview("Cửa sổ — Nhiều tệp") {
    ConvertWindow(initialState: ConvertFixtures.batch)
        .frame(width: 920, height: 620)
}
