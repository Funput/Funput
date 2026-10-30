#if canImport(Darwin)
import Darwin
#if os(iOS)
import os
#endif

/// The process's memory as the kernel accounts for it when it decides to kill it.
///
/// A keyboard extension is killed by jetsam once `physical` crosses a per-process limit
/// in the tens of megabytes. No memory warning precedes the kill, and no crash report
/// follows it, so this is the number to watch, not resident size: `physical` is what
/// jetsam compares, and it ignores clean pages the system can drop on its own.
public struct MemoryFootprint: Equatable, Sendable {
    /// `phys_footprint`: dirty and compressed memory charged to this process.
    public let physical: UInt64
    /// The highest `physical` this process has reached.
    public let peak: UInt64

    public init(physical: UInt64, peak: UInt64) {
        self.physical = physical
        self.peak = peak
    }

    /// The footprint now, or nil if the kernel refuses to answer.
    public static func current() -> MemoryFootprint? {
        var info = task_vm_info_data_t()
        var count = mach_msg_type_number_t(
            MemoryLayout<task_vm_info_data_t>.size / MemoryLayout<natural_t>.size
        )
        let result = withUnsafeMutablePointer(to: &info) { pointer in
            pointer.withMemoryRebound(to: integer_t.self, capacity: Int(count)) {
                task_info(mach_task_self_, task_flavor_t(TASK_VM_INFO), $0, &count)
            }
        }
        guard result == KERN_SUCCESS else { return nil }
        return MemoryFootprint(
            physical: info.phys_footprint,
            peak: info.ledger_phys_footprint_peak > 0
                ? UInt64(info.ledger_phys_footprint_peak) : info.phys_footprint
        )
    }

    /// How much more this process may allocate before jetsam kills it. Zero where the
    /// system sets no limit, which includes the simulator.
    public static func availableBytes() -> UInt64 {
#if os(iOS)
        UInt64(os_proc_available_memory())
#else
        0
#endif
    }

    /// `physical` in mebibytes, for logs.
    public var physicalMegabytes: Double { Double(physical) / 1_048_576 }

    /// `peak` in mebibytes, for logs.
    public var peakMegabytes: Double { Double(peak) / 1_048_576 }
}
#endif
