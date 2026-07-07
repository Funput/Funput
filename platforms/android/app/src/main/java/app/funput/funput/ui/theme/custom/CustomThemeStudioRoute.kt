package app.funput.funput.ui.theme.custom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import app.funput.funput.ime.settings.KeyboardThemeSettings
import app.funput.funput.theme.BuiltInKeyboardThemeSource
import app.funput.funput.theme.InstalledThemeRepository
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.store.custom.CustomThemeDraft
import app.funput.funput.theme.store.custom.CustomThemeInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun CustomThemeStudioRoute(
    editingThemeId: KeyboardThemeId?,
    themeRepository: InstalledThemeRepository,
    saveHandler: CustomThemeSaveHandler,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    CreateCustomThemeScreen(
        baseThemes = BuiltInKeyboardThemeSource.loadThemes(),
        editingTheme = editingThemeId?.let(themeRepository::find),
        onSave = { draft ->
            scope.launch {
                saveHandler.save(draft, editingThemeId)
                onDone()
            }
        },
        onBack = onDone,
    )
}

internal class CustomThemeSaveHandler(
    private val themeRepository: InstalledThemeRepository,
    private val installer: CustomThemeInstaller,
    private val themeSettings: KeyboardThemeSettings,
) {
    suspend fun save(draft: CustomThemeDraft, editingThemeId: KeyboardThemeId?): KeyboardThemeDescriptor {
        val descriptor = withContext(Dispatchers.IO) {
            val baseTheme = themeRepository.resolve(draft.baseThemeId)
            editingThemeId?.let { themeId ->
                installer.update(themeId, draft, baseTheme)
            } ?: installer.install(
                draft = draft,
                baseTheme = baseTheme,
                existingThemeIds = themeRepository.themes.map { theme -> theme.id }.toSet(),
            )
        }
        themeSettings.setTheme(descriptor.id)
        return descriptor
    }
}
