package app.funput.funput.ui.theme.custom

import android.content.Context
import android.content.Intent
import android.net.Uri

internal fun Context.persistBackgroundImageAccess(uri: Uri) {
    runCatching {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
}
