package app.funput.funput.ime.speech.integration.flow

import app.funput.funput.ime.speech.integration.ImeSpeechUiBinder
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.platform.SpeechMainQueue
import app.funput.funput.ime.speech.preparation.*
import app.funput.funput.ime.speech.session.SpikeBackend
import app.funput.funput.ime.speech.session.SpikeEditor
import app.funput.funput.ime.speech.session.SpikeScheduler
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.ui.speech.SpeechPanelAction
import org.junit.Assert.*
import org.junit.Test

class SpeechFlowSetupTest {
    @Test fun independentSetupDownloadMakesTheNextTapReadyWithoutWaitingForCacheExpiry() {
        for (initial in listOf(SpeechCapability.DOWNLOADABLE, SpeechCapability.PENDING)) {
            var installed = false
            val factory = FakePreparationFactory(34).apply {
                configure = { client ->
                    client.onCheck = {
                        client.languages(installed = if (installed) listOf("vi-VN") else emptyList(),
                            pending = if (!installed && initial == SpeechCapability.PENDING) listOf("vi-VN") else emptyList(),
                            supported = listOf("vi-VN"))
                    }
                    client.onDownload = {
                        installed = true
                        client.downloadListener(SpeechDownloadEvent.Success)
                    }
                }
            }
            val time = FakePreparationTime()
            val main = SpeechMainQueue { it() }
            val imeService = OnDeviceSpeechPreparationService(factory, main, time)
            val setupService = OnDeviceSpeechPreparationService(factory, main, time)
            val backend = SpikeBackend()
            val scheduler = SpikeScheduler()
            val port = FlowUiPort()
            val ui = ImeSpeechUiBinder(port)
            var setupVisits = 0
            val flow = ImeSpeechFlow(backend, SpikeEditor(), imeService, scheduler, scheduler,
                ui, { true }, { true }, { KeyboardLanguage.VIETNAMESE }, {}, { locale ->
                    setupVisits++
                    setupService.download(locale) { assertEquals(SpeechDownloadEvent.Success, it) }.start()
                })
            try {
                flow.start()
                assertTrue(port.panels.last().canOpenSetup)
                assertTrue(backend.recordings.isEmpty())
                flow.action(SpeechPanelAction.OPEN_SETUP)
                assertEquals(1, setupVisits)
                assertFalse(ui.engaged)
                assertTrue(backend.recordings.isEmpty()) // Setup never starts recording.
                flow.start()
                assertEquals(listOf(SpeechLocale.VI), backend.locales)
                assertEquals(0L, time.now) // The five-minute TTL has not elapsed.
                assertTrue(factory.clients.all { it.closes == 1 })
            } finally { flow.close() }
        }
    }
}
