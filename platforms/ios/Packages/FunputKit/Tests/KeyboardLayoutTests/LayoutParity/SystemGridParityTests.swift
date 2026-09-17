import CoreGraphics
import KeyboardLayout
import Testing

/// The system preset with system sizing, checked against the iOS 27 stock keyboard. Numbers
/// are key edges in points, measured from simulator screenshots.
struct SystemGridParityTests {
    struct Stock: Sendable, CustomTestStringConvertible {
        let width: CGFloat
        let hasNumberRow: Bool
        let shift: ClosedRange<CGFloat>
        let z: CGFloat
        let delete: ClosedRange<CGFloat>
        let symbols: ClosedRange<CGFloat>
        let space: ClosedRange<CGFloat>
        let enter: ClosedRange<CGFloat>

        var testDescription: String { "\(Int(width))pt\(hasNumberRow ? " with digits" : "")" }
    }

    static let stock: [Stock] = [
        Stock(width: 390, hasNumberRow: false, shift: 6.7...50.4, z: 64.0, delete: 339.7...383.7,
              symbols: 6.7...96.4, space: 102.3...287.6, enter: 293.7...383.7),
        Stock(width: 402, hasNumberRow: false, shift: 6.7...52.0, z: 65.7, delete: 350.0...395.7,
              symbols: 6.7...99.4, space: 105.3...296.6, enter: 302.7...395.7),
        Stock(width: 440, hasNumberRow: false, shift: 6.7...56.7, z: 71.3, delete: 383.3...433.6,
              symbols: 6.7...108.7, space: 114.7...325.4, enter: 331.3...433.6),
        Stock(width: 390, hasNumberRow: true, shift: 6.7...58.0, z: 64.0, delete: 332.0...383.7,
              symbols: 6.7...96.4, space: 102.3...287.6, enter: 293.7...383.7),
        Stock(width: 402, hasNumberRow: true, shift: 6.7...59.7, z: 65.7, delete: 342.3...395.6,
              symbols: 6.7...99.4, space: 105.3...296.6, enter: 302.7...395.7),
        Stock(width: 440, hasNumberRow: true, shift: 6.7...65.4, z: 71.3, delete: 374.7...433.7,
              symbols: 6.7...108.7, space: 114.7...325.4, enter: 331.3...433.6),
    ]

    @Test("Shift and action rows sit where Apple puts them", arguments: stock)
    func matchesStock(expected: Stock) throws {
        let layout = expected.hasNumberRow
            ? SystemKeyboardLayouts.letters(.vni)
            : SystemKeyboardLayouts.letters(.telex, showsNumberRow: false)
        let resolved = KeyboardGeometry.resolve(
            layout: layout,
            size: CGSize(width: expected.width, height: 300),
            sizing: .system
        )
        func frame(_ match: (KeySpec) -> Bool) throws -> CGRect {
            try #require(resolved.keys.first { match($0.spec) }?.frame)
        }
        try expectEdges(frame { $0.role == .shift }, expected.shift)
        try expectEdges(frame { $0.role == .backspace }, expected.delete)
        try expectEdges(frame { $0.role == .symbols }, expected.symbols)
        try expectEdges(frame { $0.role == .space }, expected.space)
        try expectEdges(frame { $0.role == .enter }, expected.enter)
        let z = try frame { $0.label == "z" }
        let s = try frame { $0.label == "s" }
        #expect(abs(z.minX - expected.z) <= 1.5)
        #expect(abs(z.minX - s.minX) <= 0.5, "z sits under s")
    }

    private func expectEdges(_ frame: CGRect, _ expected: ClosedRange<CGFloat>) {
        #expect(abs(frame.minX - expected.lowerBound) <= 1.5, "\(frame) vs \(expected)")
        #expect(abs(frame.maxX - expected.upperBound) <= 1.5, "\(frame) vs \(expected)")
    }
}
