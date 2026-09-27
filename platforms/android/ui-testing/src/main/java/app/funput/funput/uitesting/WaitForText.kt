package app.funput.funput.uitesting

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString

/**
 * Waits until [text] is on screen, for content that arrives after asynchronous work (a store read,
 * a decode). On timeout the failure carries what *was* on screen, so a wait that only times out on
 * a loaded CI runner can be diagnosed from its report instead of guessed at.
 */
fun ComposeContentTestRule.waitForText(text: String, timeoutMillis: Long = DefaultWaitMillis) {
    try {
        waitUntil(timeoutMillis) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    } catch (timeout: ComposeTimeoutException) {
        val screen = runCatching { onRoot().printToString(maxDepth = Int.MAX_VALUE) }.getOrElse { "<no root: $it>" }
        throw AssertionError("\"$text\" did not appear within $timeoutMillis ms. On screen:\n$screen", timeout)
    }
}

/** Long enough for a busy CI runner, short enough that a real hang still fails fast. */
const val DefaultWaitMillis: Long = 5_000L
