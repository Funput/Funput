package app.funput.funput.ime.speech.preparation

import app.funput.funput.ime.speech.model.SpeechLocale
import org.junit.Assert.*
import org.junit.Test

class CheckSpeechOperationTest {
    @Test fun operationDoesNothingUntilStartAndClosesBeforeFinalDelivery() {
        val f = PreparationFixture()
        val results = mutableListOf<SpeechCapability>()
        val op = f.service.check(SpeechLocale.VI) { assertEquals(1, f.client.closes); results += it }
        assertTrue(f.factory.clients.isEmpty())
        op.start()
        op.start()
        assertEquals(1, f.factory.clients.size)
        f.client.languages(installed = listOf("vi-VN"))
        f.client.languages()
        op.close()
        assertEquals(listOf(SpeechCapability.READY), results)
        assertEquals(1, f.client.closes)
        assertTrue(f.time.tasks.single().cancelled)
    }
    @Test fun everyApiGuardAvoidsClientCreation() {
        for (api in listOf(26, 30, 31, 32)) {
            val f = PreparationFixture(api)
            var result: SpeechCapability? = null
            f.service.check(SpeechLocale.VI) { result = it }.start()
            assertEquals(if (api < 31) SpeechCapability.UNSUPPORTED else SpeechCapability.UNKNOWN, result)
            assertTrue(f.factory.clients.isEmpty())
            if (api < 31) assertEquals(0, f.factory.probes)
        }
    }
    @Test fun serviceUnavailableAndProbeExceptionAreDifferent() {
        val f = PreparationFixture()
        f.factory.available = false
        assertEquals(SpeechAvailability.UNAVAILABLE, f.service.availability())
        f.factory.probeThrows = true
        assertEquals(SpeechAvailability.UNKNOWN, f.service.availability())
        f.service.check(SpeechLocale.VI) { assertEquals(SpeechCapability.UNKNOWN, it) }.start()
        assertTrue(f.factory.clients.isEmpty())
    }
    @Test fun timeoutDestroysAndSuppressesLateCallback() {
        val f = PreparationFixture()
        val results = mutableListOf<SpeechCapability>()
        f.service.check(SpeechLocale.EN) { results += it }.start()
        f.time.advance(2_999)
        assertTrue(results.isEmpty())
        f.time.advance(1)
        f.client.languages(installed = listOf("en-US"))
        assertEquals(listOf(SpeechCapability.UNKNOWN), results)
        assertEquals(1, f.client.closes)
    }
    @Test fun closingBeforeStartAndWhileCheckingSuppressesDelivery() {
        val f = PreparationFixture()
        val before = f.service.check(SpeechLocale.VI) { error("closed") }
        before.close()
        before.start()
        assertTrue(f.factory.clients.isEmpty())
        val during = f.service.check(SpeechLocale.VI) { error("closed") }
        during.start()
        during.close()
        f.client.languages(installed = listOf("vi-VN"))
        f.time.tasks.last().action()
        assertEquals(1, f.client.closes)
    }
    @Test fun synchronousCallbackCanReenterCloseWithoutDuplicateCleanup() {
        val f = PreparationFixture()
        lateinit var op: SpeechPreparationOperation
        f.factory.configure = { client -> client.onCheck = { client.languages(installed = listOf("vi-VN")) } }
        op = f.service.check(SpeechLocale.VI) { op.close() }
        op.start()
        assertEquals(1, f.client.closes)
    }
    @Test fun closeCallbackCannotReenterDelivery() {
        val f = PreparationFixture()
        var count = 0
        f.service.check(SpeechLocale.VI) { count++ }.start()
        f.client.onClose = { f.client.languages(installed = listOf("vi-VN")) }
        f.client.languages(installed = listOf("vi-VN"))
        assertEquals(1, count)
    }
    @Test fun factoryAndCommandFailureBecomeUnknownWithOwnershipPreserved() {
        val f = PreparationFixture()
        f.factory.createThrows = true
        f.service.check(SpeechLocale.VI) { assertEquals(SpeechCapability.UNKNOWN, it) }.start()
        f.factory.createThrows = false
        f.factory.configure = { it.onCheck = { error("check") } }
        f.service.check(SpeechLocale.VI) { assertEquals(SpeechCapability.UNKNOWN, it) }.start()
        assertEquals(1, f.client.closes)
    }
    @Test fun queuedStartCloseAndCallbacksExecuteInOrder() {
        val f = PreparationFixture()
        f.queued = true
        val op = f.service.check(SpeechLocale.VI) { error("closed") }
        op.start()
        op.close()
        assertTrue(f.factory.clients.isEmpty())
        f.drain()
        f.client.languages()
        f.drain()
        assertEquals(1, f.client.closes)
    }
    @Test fun immediateDeadlineStillClosesTimerAndSkipsCommand() {
        val f = PreparationFixture()
        f.time.inline = true
        f.service.check(SpeechLocale.VI) { assertEquals(SpeechCapability.UNKNOWN, it) }.start()
        assertEquals(0, f.client.checks)
        assertEquals(1, f.client.closes)
        assertTrue(f.time.tasks.single().cancelled)
    }
}
