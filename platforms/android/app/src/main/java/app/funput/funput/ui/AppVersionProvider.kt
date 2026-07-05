package app.funput.funput.ui

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

internal object AppVersionProvider {
    fun versionName(context: Context): String = runCatching {
        packageInfo(context).versionName.orEmpty()
    }.getOrDefault("").ifBlank { "—" }

    private fun packageInfo(context: Context): PackageInfo {
        val packageManager = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            legacyPackageInfo(packageManager, context.packageName)
        }
    }

    @Suppress("DEPRECATION")
    private fun legacyPackageInfo(
        packageManager: PackageManager,
        packageName: String,
    ): PackageInfo = packageManager.getPackageInfo(packageName, 0)
}
