package app.funput.funput.keyboard.ui.speech.motion

import android.view.View
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import app.funput.funput.keyboard.ui.speech.SpeechPanelFixture
import app.funput.funput.keyboard.ui.speech.SpeechPanelStage
import app.funput.funput.keyboard.ui.speech.SpeechPanelState
import app.funput.funput.keyboard.ui.speech.SpeechPanelView
import app.funput.funput.keyboard.ui.support.childOfType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SpeechOrbMotionInstrumentedTest {
    private var durationScale by mutableFloatStateOf(1f)
    @get:Rule val compose = createEmptyComposeRule(effectContext = object : MotionDurationScale {
        override val scaleFactor: Float get() = durationScale
    })

    @Test fun orbMovesWithoutActionsAndStopsWhenMotionIsDisabledDuringTheSession() {
        compose.mainClock.autoAdvance = false
        SpeechPanelFixture().use { fixture ->
            var actions = 0
            fixture.update {
                it.callbacks.onSpeechAction = { actions++ }
                it.speechPanelState = SpeechPanelState(SpeechPanelStage.LISTENING, preview = "Xin chào")
                it.showSpeechPanel()
            }
            compose.mainClock.advanceTimeBy(200)
            val first = pixels(fixture)
            compose.mainClock.advanceTimeBy(1600)
            assertFalse(first.contentEquals(pixels(fixture)))
            compose.runOnUiThread { durationScale = 0f }
            compose.mainClock.advanceTimeBy(100)
            val stationary = pixels(fixture)
            compose.mainClock.advanceTimeBy(2000)
            assertArrayEquals(stationary, pixels(fixture))
            assertEquals(0, actions)
        }
    }

    @Test fun disablingMotionBeforeShowingThePanelStillRendersTheOrb() {
        durationScale = 0f
        compose.mainClock.autoAdvance = false
        SpeechPanelFixture().use { fixture ->
            fixture.update {
                it.speechPanelState = SpeechPanelState(SpeechPanelStage.PREPARING)
                it.showSpeechPanel()
            }
            compose.mainClock.advanceTimeBy(100)
            val first = pixels(fixture)
            compose.mainClock.advanceTimeBy(1000)
            assertArrayEquals(first, pixels(fixture))
            compose.onNodeWithTag("speech-orb").assertIsDisplayed()
        }
    }

    @Test fun parentVisibilityAndPanelNavigationSuspendMotionAndReuseResumesIt() {
        SpeechPanelFixture().use { fixture ->
            fixture.update { keyboard ->
                keyboard.speechPanelState = SpeechPanelState(SpeechPanelStage.LISTENING)
                keyboard.showSpeechPanel()
                val panel = keyboard.childOfType<SpeechPanelView>()
                assertTrue(panel.isMotionVisible)
                keyboard.visibility = View.GONE
                assertFalse(panel.isMotionVisible)
                keyboard.visibility = View.VISIBLE
                assertTrue(panel.isMotionVisible)
                keyboard.showLettersPanel()
                assertFalse(panel.isMotionVisible)
                keyboard.showSpeechPanel()
                assertTrue(panel.isMotionVisible)
            }
        }
    }

    @Test fun errorOrbRemainsStaticAndTheIndicatorSurvivesPartialUpdates() {
        compose.mainClock.autoAdvance = false
        SpeechPanelFixture().use { fixture ->
            fixture.update {
                it.speechPanelState = SpeechPanelState(SpeechPanelStage.ERROR, message = "Thử lại")
                it.showSpeechPanel()
            }
            compose.mainClock.advanceTimeBy(100)
            val error = pixels(fixture)
            compose.mainClock.advanceTimeBy(2000)
            assertArrayEquals(error, pixels(fixture))
            fixture.update { it.speechPanelState = SpeechPanelState(SpeechPanelStage.LISTENING, preview = "Xin") }
            compose.mainClock.advanceTimeBy(100)
            compose.onNodeWithTag("speech-orb").assertIsDisplayed()
            fixture.update { it.speechPanelState = it.speechPanelState.copy(preview = "Xin chào") }
            compose.mainClock.advanceTimeBy(100)
            compose.onNodeWithTag("speech-orb").assertIsDisplayed()
        }
    }

    private fun pixels(fixture: SpeechPanelFixture): IntArray {
        lateinit var result: IntArray
        fixture.update { keyboard ->
            val image = Bitmap.createBitmap(keyboard.width, keyboard.height, Bitmap.Config.ARGB_8888)
            keyboard.draw(Canvas(image))
            result = IntArray(image.width * image.height)
            image.getPixels(result, 0, image.width, 0, 0, image.width, image.height)
            image.recycle()
        }
        return result
    }
}
