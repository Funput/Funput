package app.funput.funput.ui.settings.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ui.theme.FunputShapes

/**
 * The card's heading. Its badge is the last brand gradient on the page — the mark is where a
 * brand belongs, and one of them reads as identity where five read as decoration.
 */
@Composable
internal fun SetupHeroHeader(iconRes: Int, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(FunputShapes.small)
                .background(Brush.linearGradient(BrandSweep)),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
        }
    }
}

/** The one action the card is asking for, as the button Material already has for that. */
@Composable
internal fun SetupPrimaryButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = FunputShapes.small,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
    }
}
