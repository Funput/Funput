package app.funput.funput.ui.settings.enterkey

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** An app the user can open from the launcher: what it is called and its own icon. */
@Immutable
internal class InstalledApp(val packageName: String, val label: String, val icon: ImageBitmap?)

/**
 * The launcher's apps, read through `<queries>` for the launcher intent so no broad package
 * visibility is needed. Icons are decoded once at row size, never at the launcher's full size.
 */
internal object InstalledApps {
    /** Apps Funput has been checked against: Enter sends there once it is chosen. */
    val Suggested = listOf("com.facebook.orca")

    /** Vietnamese alphabetical order, so "Đ" sorts after "D" rather than after "Z". */
    val labelOrder: Collator get() = Collator.getInstance(Locale.forLanguageTag("vi"))

    suspend fun load(context: Context, iconSizePx: Int): List<InstalledApp> = withContext(Dispatchers.IO) {
        val manager = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        manager.queryIntentActivities(launcher, PackageManager.MATCH_ALL)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map { info ->
                val icon = runCatching { info.loadIcon(manager).toBitmap(iconSizePx, iconSizePx).asImageBitmap() }
                InstalledApp(info.packageName, info.loadLabel(manager).toString(), icon.getOrNull())
            }
            .sortedWith(compareBy(labelOrder) { it.label })
    }

    /** The names of [packages] for a summary line; a package no longer installed reads as itself. */
    suspend fun labels(context: Context, packages: Set<String>): List<String> = withContext(Dispatchers.IO) {
        val manager = context.packageManager
        packages.map { packageName ->
            runCatching { manager.getApplicationInfo(packageName, 0).loadLabel(manager).toString() }
                .getOrDefault(packageName)
        }.sortedWith(labelOrder)
    }
}
