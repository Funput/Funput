package app.funput.funput.ui.about.licenses

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LicensesRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val loading = stringResource(R.string.licenses_loading)
    val error = stringResource(R.string.licenses_error)
    val notice by produceState(loading, context, error) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                NoticeAssets.joinToString(separator = "\n\n") { path ->
                    context.assets.open(path).bufferedReader().use { it.readText() }
                }
            }.getOrDefault(error)
        }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.licenses_title)) }, navigationIcon = {
            TextButton(onClick = onBack) { Text(stringResource(R.string.licenses_back)) }
        })
    }) { padding ->
        SelectionContainer(Modifier.padding(padding).fillMaxSize()) {
            Text(notice, modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp))
        }
    }
}

/**
 * Third-party notices shipped as assets, shown in this order: the dictionary's notice (generated
 * by `:ime`), then the licences of FunputUI's bundled Be Vietnam Pro font and Phosphor icons.
 */
private val NoticeAssets = listOf(
    "lexicon/NOTICE.md",
    "licenses/be-vietnam-pro-OFL.txt",
    "licenses/phosphor-MIT.txt",
)
