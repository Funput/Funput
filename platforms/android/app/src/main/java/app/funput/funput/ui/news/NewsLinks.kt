package app.funput.funput.ui.news

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import app.funput.funput.ui.keyboard.openWebsite

/**
 * Opens a post or guide in a Custom Tab, over the app the way iOS shows it in a Safari sheet, so
 * back returns to the list. Without a browser that offers Custom Tabs it opens like any link.
 */
internal fun Context.openArticle(url: String) {
    try {
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(this, url.toUri())
    } catch (missingBrowser: ActivityNotFoundException) {
        openWebsite(url)
    }
}
