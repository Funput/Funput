/// The kind of device a layout is built for.
///
/// Kept out of UIKit so the layout module stays platform-neutral: the keyboard extension
/// decides from the device idiom and hands the answer in. `.phone` is the default
/// everywhere, so a caller that never mentions it keeps today's layouts.
public enum KeyboardFormFactor: String, CaseIterable, Hashable, Sendable {
    case phone
    case pad
}
