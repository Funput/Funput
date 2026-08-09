package app.funput.funput.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the failure mode this scheme was written to end: a role nobody assigned keeps Material's
 * baseline purple and leaks violet into a dialog or an elevation tint. Reflection is used on
 * purpose — an explicit list of roles would go stale the moment Material adds one.
 */
class FunputColorSchemeTest {

    @Test
    fun `light scheme assigns every role`() {
        assertEquals(emptyList<String>(), unassignedRoles(FunputLightColors, lightColorScheme()))
    }

    @Test
    fun `dark scheme assigns every role`() {
        assertEquals(emptyList<String>(), unassignedRoles(FunputDarkColors, darkColorScheme()))
    }

    private fun unassignedRoles(scheme: ColorScheme, baseline: ColorScheme): List<String> =
        roleNames().filter { role ->
            role !in SharedWithBaseline && readRole(scheme, role) == readRole(baseline, role)
        }

    /**
     * Roles that match the Material baseline because both land on pure white or black, not because
     * they were forgotten. Anything else matching means a missing assignment.
     */
    private val SharedWithBaseline = setOf(
        "onPrimary",
        "onSecondary",
        "onTertiary",
        "onError",
        "surfaceContainerLowest",
        "scrim",
    )

    /**
     * [ColorScheme] roles are `Color` values, and `Color` is a value class, so Kotlin mangles the
     * getters to `getPrimary-0d7_KjU` and returns the packed `long`. Read them through those.
     */
    private fun roleGetters() = ColorScheme::class.java.declaredMethods
        .filter { it.parameterCount == 0 && it.returnType == java.lang.Long.TYPE }
        .filter { it.name.startsWith("get") }

    private fun roleNames(): List<String> = roleGetters()
        .map { it.name.removePrefix("get").substringBefore('-').replaceFirstChar(Char::lowercase) }
        .distinct()
        .sorted()

    private fun readRole(scheme: ColorScheme, role: String): Long {
        val getter = roleGetters().first {
            it.name.removePrefix("get").substringBefore('-')
                .replaceFirstChar(Char::lowercase) == role
        }
        return getter.invoke(scheme) as Long
    }
}
