import FunputShared
import Testing

struct MemoryFootprintTests {
    @Test("The footprint is readable and the peak never trails it")
    func readable() throws {
        let footprint = try #require(MemoryFootprint.current())
        #expect(footprint.physical > 0)
        #expect(footprint.peak >= footprint.physical)
    }

    @Test("Touching memory raises the footprint by about as much")
    func tracksDirtyMemory() throws {
        let size = 16 * 1_048_576
        let before = try #require(MemoryFootprint.current())
        let buffer = UnsafeMutableRawBufferPointer.allocate(byteCount: size, alignment: 16)
        defer { buffer.deallocate() }
        // Untouched pages are not charged; writing them is what makes them dirty.
        buffer.initializeMemory(as: UInt8.self, repeating: 1)
        let after = try #require(MemoryFootprint.current())
        #expect(after.physical >= before.physical + UInt64(size) / 2)
    }
}
