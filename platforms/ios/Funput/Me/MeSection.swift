/// The groups of the "Tôi" tab, in display order.
///
/// This tab is where features that are about the person rather than the keyboard will
/// land (an account, sync); each becomes a case here, placed where it should appear.
enum MeSection: CaseIterable, Identifiable {
    case data
    case discovery
    case support
    case legal
    case licenses

    var id: Self { self }
}
