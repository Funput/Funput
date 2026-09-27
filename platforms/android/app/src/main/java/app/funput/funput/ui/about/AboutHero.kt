package app.funput.funput.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.theme.FunputTint
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.kit.theme.tint

/** Mark, name and version, with no card around them: the page below carries all the containers. */
@Composable
internal fun AboutHero(versionName: String) {
    val version = stringResource(R.string.about_version, versionName)
    val name = stringResource(R.string.app_name)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.tight),
        // One announcement for the mark and its version, rather than three fragments.
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "$name, $version" },
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(96.dp),
        )
        BasicText(name, style = FunputUi.typography.title.copy(color = FunputUi.colors.label))
        BasicText(version, style = FunputUi.typography.label.copy(color = FunputUi.colors.secondaryLabel))
    }
}

/** Who Funput is for, closing the page: a heart, a line, and the promise that it stays free. */
@Composable
internal fun AboutFooter() {
    val colors = FunputUi.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.tight),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FunputUi.spacing.small),
        ) {
            Image(
                painter = painterResource(FunputIcons.Heart),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.tint(FunputTint.PINK)),
                modifier = Modifier.size(16.dp),
            )
            BasicText(
                stringResource(R.string.about_footer_title),
                style = FunputUi.typography.label.copy(color = colors.secondaryLabel),
            )
        }
        BasicText(
            stringResource(R.string.about_footer_body),
            style = FunputUi.typography.caption.copy(color = colors.secondaryLabel, textAlign = TextAlign.Center),
        )
    }
}
