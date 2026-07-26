package app.funput.funput.ime.settings

import androidx.datastore.preferences.core.mutablePreferencesOf
import app.funput.funput.theme.KeyboardThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardThemeSelectionTest {
    @Test
    fun aStoredSingleThemeSeedsEverySlotSoAnUpgradeChangesNothing() {
        val ocean = KeyboardThemeId.of("custom.ocean")
        val preferences = mutablePreferencesOf(
            KeyboardThemeSettings.ThemeIdKey to ocean.value,
        )

        val selection = KeyboardThemeSettingCodec.decode(preferences)

        assertEquals(ocean, selection.singleThemeId)
        assertEquals(ocean, selection.lightThemeId)
        assertEquals(ocean, selection.darkThemeId)
        assertFalse(selection.followsAppearance)
        assertEquals(ocean, selection.resolve(darkAppearance = true))
        assertEquals(ocean, selection.resolve(darkAppearance = false))
    }

    @Test
    fun emptyPreferencesResolveToTheDefaultTheme() {
        val selection = KeyboardThemeSettingCodec.decode(mutablePreferencesOf())

        assertEquals(KeyboardThemeSettings.DefaultThemeId, selection.resolve(darkAppearance = true))
    }

    @Test
    fun followingAppearancePicksThePerModeSlots() {
        val preferences = mutablePreferencesOf(
            KeyboardThemeSettings.ThemeIdKey to KeyboardThemeId.Dark.value,
            KeyboardThemeSettings.LightThemeIdKey to KeyboardThemeId.Light.value,
            KeyboardThemeSettings.DarkThemeIdKey to KeyboardThemeId.Dark.value,
            KeyboardThemeSettings.FollowsAppearanceKey to true,
        )

        val selection = KeyboardThemeSettingCodec.decode(preferences)

        assertTrue(selection.followsAppearance)
        assertEquals(KeyboardThemeId.Light, selection.resolve(darkAppearance = false))
        assertEquals(KeyboardThemeId.Dark, selection.resolve(darkAppearance = true))
    }

    @Test
    fun notFollowingIgnoresThePerModeSlots() {
        val ocean = KeyboardThemeId.of("custom.ocean")
        val selection = KeyboardThemeSelection(
            singleThemeId = ocean,
            lightThemeId = KeyboardThemeId.Light,
            darkThemeId = KeyboardThemeId.Dark,
            followsAppearance = false,
        )

        assertEquals(ocean, selection.resolve(darkAppearance = true))
        assertEquals(ocean, selection.resolve(darkAppearance = false))
    }

    @Test
    fun aMalformedSlotFallsBackToTheSingleTheme() {
        val ocean = KeyboardThemeId.of("custom.ocean")
        val preferences = mutablePreferencesOf(
            KeyboardThemeSettings.ThemeIdKey to ocean.value,
            KeyboardThemeSettings.DarkThemeIdKey to "not a theme id",
        )

        assertEquals(ocean, KeyboardThemeSettingCodec.decode(preferences).darkThemeId)
    }

    @Test
    fun withThemeWritesOnlyTheTargetedSlot() {
        val ocean = KeyboardThemeId.of("custom.ocean")

        val selection = KeyboardThemeSelection().withTheme(ocean, KeyboardThemeSlot.DARK)

        assertEquals(ocean, selection.darkThemeId)
        assertEquals(KeyboardThemeId.Light, selection.lightThemeId)
        assertEquals(KeyboardThemeId.Default, selection.singleThemeId)
    }

    @Test
    fun slotsUsingFindsEverySlotPointingAtADeletedTheme() {
        val ocean = KeyboardThemeId.of("custom.ocean")
        val selection = KeyboardThemeSelection(singleThemeId = ocean, darkThemeId = ocean)

        assertEquals(
            listOf(KeyboardThemeSlot.SINGLE, KeyboardThemeSlot.DARK),
            selection.slotsUsing(ocean),
        )
    }
}
