@testable import KeyboardTouchCore

func contact(_ value: UInt64) -> ContactID {
    ContactID(rawValue: value)
}

func emittedPayloads(
    _ emissions: [PressEmission<String>]
) -> [String] {
    emissions.map(\.payload)
}

struct DeterministicRandom {
    private var state: UInt64

    init(seed: UInt64) {
        state = seed
    }

    mutating func next(upTo bound: Int) -> Int {
        precondition(bound > 0)
        state &+= 0x9E37_79B9_7F4A_7C15
        var value = state
        value = (value ^ (value >> 30)) &* 0xBF58_476D_1CE4_E5B9
        value = (value ^ (value >> 27)) &* 0x94D0_49BB_1331_11EB
        return Int((value ^ (value >> 31)) % UInt64(bound))
    }
}
