package app.funput.funput.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import app.funput.funput.R

/**
 * Be Vietnam Pro, downloaded rather than bundled.
 *
 * Chosen over a neutral grotesque because it is drawn for this language: the stacked marks Funput
 * exists to type — ế, ự, ỡ — are designed rather than assembled from a Latin glyph and a floating
 * accent, and they are what the reader looks at all day.
 *
 * Downloadable Fonts costs nothing in the APK and the provider caches across apps. It needs Google
 * Play services, which the Play-only Android release can assume; where it is missing the family
 * falls back to the platform sans, which is what every screen used until now anyway.
 */
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val BeVietnamPro = GoogleFont("Be Vietnam Pro")

internal val FunputFontFamily = FontFamily(
    Font(googleFont = BeVietnamPro, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = BeVietnamPro, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = BeVietnamPro, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = BeVietnamPro, fontProvider = provider, weight = FontWeight.Bold),
)
