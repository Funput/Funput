@_spi(Testing) import KeyboardInput
import Testing

@MainActor
struct KeyboardDocumentSynchronizerRobustnessTests {
    @Test("Authored echoes are exact after context normalization")
    func exactEchoMatching() {
        var driver = KeyboardDocumentSynchronizerTestDriver(context: "prefix-foobar")
        driver.insert("x")

        let before = driver.consumeEcho("prefix-foobar")
        let current = driver.consumeEcho("prefix-foobarx")
        let looseSuffix = driver.consumeEcho("foobar")
        #expect(before)
        #expect(current)
        #expect(!looseSuffix)
    }

    @Test("Only the current and immediately previous word epochs survive")
    func boundedWordEpochs() {
        var driver = KeyboardDocumentSynchronizerTestDriver()
        driver.insert("mot")
        driver.insert(" ", closesEpoch: true)
        driver.insert("hai")
        let firstEpochRetained = driver.consumeEcho("mot")
        #expect(firstEpochRetained)

        driver.insert(" ", closesEpoch: true)
        driver.insert("ba")
        let firstEpochExpired = driver.consumeEcho("mot")
        #expect(!firstEpochExpired)
        #expect(driver.pendingContextCount <= 64)
    }

    @Test("External correction replaces the shadow and resets active composition")
    func externalCorrection() {
        var driver = KeyboardDocumentSynchronizerTestDriver(context: "helo")
        driver.insert("x")
        driver.acceptExternal("hello")

        #expect(driver.snapshotContext == "hello")
        let delayedEcho = driver.consumeEcho("helox")
        #expect(delayedEcho)
        #expect(driver.snapshotContext == "hello")
        #expect(driver.requiresReset(context: "hello!", buffer: "hello"))
    }

    @Test("Selection forces active composition to reset")
    func selectionReset() {
        let driver = KeyboardDocumentSynchronizerTestDriver(context: "xin")
        #expect(driver.requiresReset(context: "xin", buffer: "xin", hasSelection: true))
    }

    @Test("Ten thousand mutations keep the ledger bounded")
    func ledgerRemainsBounded() {
        var driver = KeyboardDocumentSynchronizerTestDriver()
        for _ in 0..<10_000 { driver.insert("a") }

        #expect(driver.pendingContextCount <= 64)
        #expect(driver.snapshotContext?.count == 128)
    }
}
