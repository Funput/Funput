import SwiftUI

struct ConvertWindow: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var store: ConvertStore

    init(initialState: ConvertScreenState? = nil) {
        _store = State(initialValue: ConvertStore(initialState: initialState))
    }

    var body: some View {
        ZStack {
            Color(nsColor: .windowBackgroundColor)
            VietnameseFlowBackground(animated: false)
                .opacity(store.state.mode == .empty ? 0.12 : 0)

            VStack(spacing: Theme.Spacing.lg) {
                ConvertHeader(
                    showsRestart: store.state.mode != .empty,
                    restart: restart
                )
                content
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .padding(Theme.Spacing.xl)
            .disabled(store.state.isBusy)

            if store.state.mode == .empty && !store.state.progress.isEmpty {
                ConvertStatus(text: store.state.progress, isBusy: store.state.isBusy)
                    .padding(Theme.Spacing.xl)
                    .frame(maxHeight: .infinity, alignment: .bottom)
            }
        }
        .tint(Theme.accent)
        .frame(minWidth: 760, minHeight: 520)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.22), value: store.state.mode)
        .dropDestination(for: URL.self) { urls, _ in
            guard !store.state.isBusy, urls.contains(where: \.isFileURL) else { return false }
            dispatch(.receiveFiles(urls.filter(\.isFileURL)))
            return true
        }
        .alert("Chuyển mã", isPresented: errorBinding) {
            Button("Đóng", role: .cancel) { store.dismissError() }
        } message: {
            Text(store.state.errorMessage ?? "")
        }
    }

    @ViewBuilder private var content: some View {
        switch store.state.mode {
        case .empty:
            ConvertEmptyView(dispatch: dispatch)
        case .text:
            ConvertTextView(state: store.state, dispatch: dispatch)
        case .files:
            ConvertBatchView(state: store.state, dispatch: dispatch)
        }
    }

    private func dispatch(_ action: ConvertAction) {
        store.send(action)
    }

    private func restart() {
        dispatch(.restart)
    }

    private var errorBinding: Binding<Bool> {
        Binding(get: { store.state.errorMessage != nil }, set: { if !$0 { store.dismissError() } })
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
