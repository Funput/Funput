package app.funput.funput.ui.theme.custom.color

import app.funput.funput.theme.KeyboardTheme

/**
 * Reading and writing colours through the follow relationships [ThemeColorRole.follows] declares.
 *
 * A role that still matches its source is following it. That is inferred rather than stored, so a
 * theme loaded from disk or edited by hand describes itself correctly without carrying extra
 * fields that could disagree with the colours they describe.
 */
internal object ThemeColorLinks {

    fun isAutomatic(role: ThemeColorRole, theme: KeyboardTheme): Boolean {
        val source = role.follows ?: return false
        return role.read(theme) == source.read(theme)
    }

    /**
     * Writes [color] to [role], carrying every role that was following it along.
     *
     * Without this, "automatic" would be a label that stopped being true the moment its source
     * changed — the follower would keep the old colour and silently become a manual choice nobody
     * made.
     */
    fun write(theme: KeyboardTheme, role: ThemeColorRole, color: Int): KeyboardTheme {
        val followers = ThemeColorRole.entries.filter { candidate ->
            candidate.follows == role && isAutomatic(candidate, theme)
        }
        return followers.fold(role.write(theme, color)) { updated, follower ->
            follower.write(updated, color)
        }
    }

    /** Puts a role back under its source's control. */
    fun restoreAutomatic(theme: KeyboardTheme, role: ThemeColorRole): KeyboardTheme {
        val source = role.follows ?: return theme
        return role.write(theme, source.read(theme))
    }
}
