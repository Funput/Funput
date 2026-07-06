package app.funput.funput.ime

/** Stable Android IME component id shared by the settings app and inspectors. */
object FunputImeComponent {
    const val PACKAGE = "app.funput.funput"
    const val SERVICE = "app.funput.funput.ime.FunputInputMethodService"
    const val SERVICE_SIMPLE_NAME = "FunputInputMethodService"

    /** Fully qualified id: `package/class`. */
    val id: String
        get() = "$PACKAGE/$SERVICE"

    /** Short id used by Android Settings on many devices: `package/.ime.Service`. */
    val idShort: String
        get() = "$PACKAGE/.ime.$SERVICE_SIMPLE_NAME"

    fun matches(raw: String?): Boolean {
        val candidate = raw?.trim()?.substringBefore(':') ?: return false
        if (candidate == id || candidate == idShort) return true
        val slash = candidate.indexOf('/')
        if (slash <= 0) return false
        val pkg = candidate.substring(0, slash)
        val cls = candidate.substring(slash + 1)
        if (pkg != PACKAGE) return false
        return cls == SERVICE ||
            cls == ".ime.$SERVICE_SIMPLE_NAME" ||
            cls.endsWith(SERVICE_SIMPLE_NAME)
    }
}
