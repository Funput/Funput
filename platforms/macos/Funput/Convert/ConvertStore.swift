import Foundation
import Observation

@MainActor @Observable
final class ConvertStore {
    private(set) var state: ConvertScreenState
    @ObservationIgnored private let worker: ConvertWorker?
    @ObservationIgnored private let platform: ConvertPlatform
    @ObservationIgnored private var textTask: Task<Void, Never>?
    @ObservationIgnored private var pendingInput: String?
    @ObservationIgnored private var generation = 0

    init(initialState: ConvertScreenState? = nil, platform: ConvertPlatform? = nil) {
        let worker = ConvertWorker()
        self.worker = worker
        self.platform = platform ?? ConvertPlatform()
        state = initialState ?? .empty(charsets: ConvertWorker.loadCharsets())
        if worker == nil { state.errorMessage = "Không khởi tạo được bộ chuyển mã." }
    }
    func send(_ action: ConvertAction) {
        guard !state.isBusy else { return }
        switch action {
        case .paste: paste()
        case .pickFiles: if let urls = platform.pickFiles() { scan(urls) }
        case let .receiveFiles(urls): scan(urls)
        case .restart: restart()
        case let .setInput(text): edit(text)
        case let .setSource(value): selectSource(value)
        case let .setTarget(value): selectTarget(value)
        case let .setRowSource(id, source): selectRow(id, source)
        case .copyResult: copyResult()
        case .saveResult: saveResult()
        case .convertFiles: convertFiles()
        case .loadMore: loadMore()
        }
    }
    func dismissError() { state.errorMessage = nil }
    private func paste() {
        guard let text = platform.pastedText() else {
            state.errorMessage = "Clipboard không có văn bản để dán."
            return
        }
        state.inputText = text
        update { await $0.setInput(text) }
    }
    private func edit(_ text: String) {
        state.inputText = text
        pendingInput = text
        generation += 1
        let token = generation
        textTask?.cancel()
        textTask = Task { [weak self] in
            try? await Task.sleep(for: .milliseconds(150))
            guard !Task.isCancelled, let self, token == generation,
                  let worker else { return }
            pendingInput = nil
            let next = await worker.setInput(text)
            guard token == generation else { return }
            state = next
        }
    }
    private func restart() {
        pendingInput = nil
        textTask?.cancel()
        update { await $0.reset() }
    }
    private func selectSource(_ source: Int?) {
        updateFlushing { await $0.setSource(source, input: $1) }
    }
    private func selectTarget(_ target: Int) {
        updateFlushing { await $0.setTarget(target, input: $1) }
    }
    private func selectRow(_ row: Int, _ source: Int?) {
        update { await $0.setRowSource(row: row, source: source, input: self.state.inputText) }
    }
    private func loadMore() {
        let count = min(state.files.count + 500, state.rowsTotal)
        update { await $0.loadRows(count, input: self.state.inputText) }
    }
    private func scan(_ urls: [URL]) {
        guard !urls.isEmpty else { return }
        pendingInput = nil
        textTask?.cancel()
        busy("Đang đọc \(urls.count) mục…") { worker in await worker.scan(urls) }
    }
    private func copyResult() {
        taskFlushing { [weak self] worker, _, token in
            let text = await worker.resultText()
            guard let self, token == generation else { return }
            state.progress = platform.copy(text) ? "Đã chép kết quả" : "Không chép được kết quả"
        }
    }

    private func saveResult() {
        taskFlushing { [weak self] worker, _, token in
            let bytes = await worker.saveBytes()
            guard let self, token == generation else { return }
            do { if try platform.save(bytes) { state.progress = "Đã lưu tệp" } }
            catch { state.errorMessage = "Không lưu được tệp: \(error.localizedDescription)" }
        }
    }

    private func convertFiles() {
        busy("Đang chuyển \(state.ready) tệp…") { worker in
            guard let result = await worker.runBatch(input: self.state.inputText) else { return nil }
            var next = result.0
            next.progress = result.1
            return next
        }
    }

    private func update(_ operation: @escaping (ConvertWorker) async -> ConvertScreenState) {
        generation += 1; let token = generation
        Task { guard let worker else { return }; let next = await operation(worker)
            if token == generation { state = next }
        }
    }

    private func updateFlushing(_ operation: @escaping (ConvertWorker, String) async -> ConvertScreenState) {
        taskFlushing { worker, input, token in
            let next = await operation(worker, input)
            if token == self.generation { self.state = next }
        }
    }

    private func taskFlushing(_ operation: @escaping (ConvertWorker, String, Int) async -> Void) {
        generation += 1; let token = generation; textTask?.cancel()
        let pending = pendingInput; pendingInput = nil; let input = state.inputText
        Task { guard let worker else { return }
            if let pending {
                let next = await worker.setInput(pending)
                guard token == generation else { return }
                state = next
            }
            guard token == generation else { return }; await operation(worker, input, token)
        }
    }

    private func busy(_ message: String, operation: @escaping (ConvertWorker) async -> ConvertScreenState?) {
        generation += 1; let token = generation; state.isBusy = true; state.progress = message
        Task { guard let worker else { state.isBusy = false; return }; let next = await operation(worker)
            guard token == generation else { return }
            state = next ?? state; state.isBusy = false
            if next == nil { state.errorMessage = "Không hoàn tất được thao tác chuyển mã." }
            else if state.mode == .empty && !state.unreadable.isEmpty {
                state.errorMessage = state.unreadable
            }
        }
    }
}
