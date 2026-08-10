package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ui.theme.custom.background.ThemeBackgroundTab

/** Choosing and framing the background image, with the keyboard it applies to above it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeBackgroundScreen(
    state: ThemeDraftState,
    onChooseImage: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.custom_theme_background_row)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.custom_theme_back_description),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            ThemeStudioPreview(
                theme = state.theme,
                backgroundImage = state.backgroundImage,
                editingThemeId = null,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            ThemeBackgroundTab(state = state, onChooseImage = onChooseImage)
        }
    }
}
