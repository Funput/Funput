package app.funput.funput.ime.suggestions

import android.os.Build
import android.os.Trace

internal inline fun <T> suggestionTrace(section: String, operation: () -> T): T {
    Trace.beginSection(section)
    return try {
        operation()
    } finally {
        Trace.endSection()
    }
}

internal fun suggestionCounter(name: String, value: Long) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Trace.setCounter(name, value)
}
