package app.funput.funput.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import app.funput.funput.R

/** Mark and version, with no card around them — the page below carries all the containers. */
@Composable
internal fun AboutHero(versionName: String, modifier: Modifier = Modifier) {
    val version = stringResource(R.string.about_version, versionName)
    val label = stringResource(R.string.app_name)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            // One announcement for the mark and its version, rather than three fragments.
            .clearAndSetSemantics { contentDescription = "$label, $version" },
    ) {
        Image(
            painter = painterResource(R.drawable.img_funput_logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(112.dp),
        )
        Text(text = label, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = version,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
