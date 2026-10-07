import Foundation

extension ClipboardCaptureController {
    /// Waits until no read is in flight and no follow-up read is queued. Production
    /// code never needs this — results arrive through `onUpdate` — but tests and
    /// diagnostics need a point where the controller has settled.
    public func settle() async {
        // A cancelled resample can leave its handle behind, so remember what was
        // already awaited instead of looping on it.
        var awaited = Set<Task<Void, Never>>()
        while true {
            if let task = readTask, awaited.insert(task).inserted {
                await task.value
            } else if let task = resampleTask, awaited.insert(task).inserted {
                await task.value
            } else {
                return
            }
        }
    }

    /// The read runs in a task so a slow provider — a Universal Clipboard item still
    /// on its way from the Mac — never holds the main thread. Every guard after the
    /// `await` exists because the world may have moved on while it was pending.
    func startRead(of before: ClipboardSnapshot) {
        isReading = true
        let session = generation
        readTask = Task { @MainActor [weak self] in
            guard let self else { return }
            let text = await gateway.readText()
            readTask = nil
            isReading = false
            finishRead(text, before: before, session: session)
            schedulePendingRead()
        }
    }

    private func finishRead(_ text: String?, before: ClipboardSnapshot, session: Int) {
        guard active, session == generation, allowsCapture() else { return }
        // A paste through the chip, or clearing the history, may have settled this
        // generation while the read was still in flight.
        guard before.changeCount != processed,
              before.changeCount != suppressed else { return }
        // Do not label an old provider result with a newer clipboard generation.
        guard gateway.snapshot().changeCount == before.changeCount else { pending = true; return }
        guard let text else { fail(); return }
        guard !text.isEmpty else { processed = before.changeCount; return }
        if save(ClipboardItem(text: text, sourceChangeCount: before.changeCount)) {
            processed = before.changeCount
            onUpdate()
        } else { fail() }
    }

    private func schedulePendingRead() {
        guard pending, active else { return }
        pending = false
        let session = generation
        resampleTask?.cancel()
        resampleTask = Task { @MainActor [weak self] in
            await Task.yield()
            guard !Task.isCancelled, let self, generation == session else { return }
            resampleTask = nil
            synchronize()
        }
    }
}
