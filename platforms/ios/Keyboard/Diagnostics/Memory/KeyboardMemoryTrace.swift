import FunputShared
import os

/// Records the extension's footprint at the moments that move it.
///
/// The keyboard is killed without warning once its footprint crosses the extension
/// limit, so the only way to see how close it runs is to sample at the transitions:
/// activation, a panel opening or being released, a memory warning. Read them with
/// `log stream --predicate 'subsystem == "app.funput.keyboard" AND category == "Memory"'`
/// or as `Footprint` events in Instruments.
enum KeyboardMemoryTrace {
    private static let log = OSLog(subsystem: "app.funput.keyboard", category: "Memory")

    static func record(_ moment: String) {
        guard let footprint = MemoryFootprint.current() else { return }
        os_signpost(
            .event, log: log, name: "Footprint",
            "moment=%{public}@ physicalMB=%{public}.1f peakMB=%{public}.1f availableMB=%{public}.1f",
            moment,
            footprint.physicalMegabytes,
            footprint.peakMegabytes,
            Double(MemoryFootprint.availableBytes()) / 1_048_576
        )
    }
}
