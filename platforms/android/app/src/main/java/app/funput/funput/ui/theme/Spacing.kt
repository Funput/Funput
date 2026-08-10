package app.funput.funput.ui.theme

import androidx.compose.ui.unit.dp

/**
 * The only distances the app is allowed to put between things.
 *
 * Before this there were 20, 18, 16, 14, 12, 10 and 6 in use, chosen one call site at a time. No
 * one can point at that and say what is wrong, and everyone can feel it: nothing lines up with
 * anything, and every new control invents its own gap.
 */
internal object Spacing {
    /** Between a label and what it labels. */
    val Tight = 4.dp

    /** Between rows of one thing. */
    val Small = 8.dp

    /** Inside a control: an icon and its text, a card and its content. */
    val Medium = 12.dp

    /** The page's own margin, and the padding inside a row. */
    val Large = 16.dp

    /** Between one section and the next. */
    val Section = 24.dp
}
