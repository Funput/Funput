import Foundation

/// When this device last booted, as seconds since the epoch.
///
/// `UIPasteboard.changeCount` restarts at boot, so a change count remembered on disk
/// only means something within the boot that wrote it. Pairing the two is what lets
/// the keyboard trust a remembered generation instead of re-reading the pasteboard —
/// and a pasteboard read is user-visible, so trusting it is the whole point.
///
/// The clock can shift this by a second or two after a time sync. That direction is
/// safe: the marks are simply treated as another boot's and re-read once.
public enum ClipboardBootTime {
    /// nil when the kernel would not say. Failing safe matters more than answering:
    /// a sentinel could equal one stored by an earlier failed probe, which would make
    /// another boot's marks look current and skip a clipboard the user did copy.
    public static var current: Int? {
        var boot = timeval()
        var size = MemoryLayout<timeval>.stride
        var mib: [Int32] = [CTL_KERN, KERN_BOOTTIME]
        guard sysctl(&mib, 2, &boot, &size, nil, 0) == 0 else { return nil }
        return Int(boot.tv_sec)
    }
}
