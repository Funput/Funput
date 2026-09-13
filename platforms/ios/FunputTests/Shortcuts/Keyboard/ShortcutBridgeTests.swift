import FunputEngine
import FunputShared
import Testing

@MainActor
struct ShortcutBridgeTests {
    @Test func fullOutputAndBuffer() {
        let composer = FunputComposer()
        composer.setEnabled(false)
        let trigger = String(repeating: "a", count: 200)
        let expansion = String(repeating: "Địa chỉ 😀\n", count: 100)
        composer.addShortcut(trigger: trigger, expansion: expansion)
        for scalar in trigger.unicodeScalars { composer.process(scalar) }
        #expect(composer.buffer() == trigger)
        let result = composer.process(" ")
        #expect(result.text == expansion + " ")
        #expect(result.deleteCount == 200)
        #expect(composer.buffer().isEmpty)
    }

    @Test func switchesAndReplacement() {
        let composer = FunputComposer()
        composer.setEnabled(false)
        composer.addShortcut(trigger: "vn", expansion: "việt nam")
        composer.setShortcutsInEnglish(false)
        for scalar in "vn ".unicodeScalars { #expect(composer.process(scalar).action == .none) }
        composer.setShortcutsInEnglish(true)
        composer.setShortcutSmartCase(false)
        for scalar in "VN ".unicodeScalars { #expect(composer.process(scalar).action == .none) }
        composer.setShortcutSmartCase(true)
        for scalar in "VN".unicodeScalars { composer.process(scalar) }
        #expect(composer.process(" ").text == "VIỆT NAM ")
        composer.setShortcutsEnabled(false)
        for scalar in "vn ".unicodeScalars { #expect(composer.process(scalar).action == .none) }
        composer.setShortcutsEnabled(true)
        composer.clearShortcuts()
        for scalar in "vn ".unicodeScalars { #expect(composer.process(scalar).action == .none) }
    }

    @Test func longExpansionLatency() {
        let expansion = String(repeating: "Dòng tiếng Việt 😀\n", count: 100)
        let clock = ContinuousClock()
        var timings: [Double] = []
        for _ in 0..<100 {
            let rig = ShortcutTypingRig(library: .init(entries: [.init(trigger: "vn", expansion: expansion)]))
            rig.type("vn")
            let elapsed = clock.measure { rig.type(" ") }.components
            timings.append(Double(elapsed.seconds) * 1_000 + Double(elapsed.attoseconds) / 1e15)
            #expect(rig.writer.text == expansion + " ")
        }
        timings.sort()
        print("Shortcut transaction 1,800 characters, 100 samples: median=\(timings[50])ms p95=\(timings[95])ms")
    }

}
