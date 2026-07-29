package app.funput.funput.keyboard.popover.model

internal object VietnameseKeyAlternates {
    fun valuesFor(character: Char): List<KeyAlternate> =
        Catalog[character.lowercaseChar()].orEmpty().map(::KeyAlternate)

    private val Catalog = mapOf(
        'a' to groups("aáàảãạ", "ăắằẳẵặ", "âấầẩẫậ"),
        'e' to groups("eéèẻẽẹ", "êếềểễệ"),
        'i' to groups("iíìỉĩị"),
        'o' to groups("oóòỏõọ", "ôốồổỗộ", "ơớờởỡợ"),
        'u' to groups("uúùủũụ", "ưứừửữự"),
        'y' to groups("yýỳỷỹỵ"),
        'd' to groups("dđ"),
    )

    private fun groups(vararg values: String): List<String> =
        values.flatMap { group -> group.map(Char::toString) }
}
