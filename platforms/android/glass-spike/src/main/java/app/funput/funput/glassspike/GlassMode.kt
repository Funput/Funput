package app.funput.funput.glassspike

/** The techniques under comparison; picked with `am start --es mode <name>`. */
enum class GlassMode(val label: String) {
    /** Tier 1: a translucent solid surface with a hairline, no sampling of what is behind. */
    SOLID("Mờ đục (tầng 1)"),

    /** Tier 2: Haze 2.0 backdrop blur (RenderEffect, API 31+). */
    HAZE("Haze blur (tầng 2)"),

    /** Tier 3: Kyant backdrop, blur + AGSL lens refraction + specular highlight (API 33+). */
    LIQUID("Liquid Glass (tầng 3)"),
    ;

    companion object {
        /** The mode named [name], case-insensitive, defaulting to [LIQUID]. */
        fun parse(name: String?): GlassMode =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: LIQUID
    }
}
