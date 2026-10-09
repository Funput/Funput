package app.funput.funput.ui.about.licenses

import org.junit.Assert.assertEquals
import org.junit.Test

/** How notices are split into what the licences screen draws. */
class LicenseTextTest {
    @Test
    fun `markdown gives headings, joined paragraphs and reflowed quotes`() {
        val notice = """
            # Third-party data

            `en.tsv` is derived from
            <https://github.com/en-wl/wordlist>.

            ```text
            Copyright 2000 by Kevin

            Permission to use,  copy
              and modify.
            ```
        """.trimIndent()

        assertEquals(
            listOf(
                LicenseBlock.Heading("Third-party data", level = 1),
                LicenseBlock.Paragraph("en.tsv is derived from https://github.com/en-wl/wordlist."),
                LicenseBlock.Quote("Copyright 2000 by Kevin\n\nPermission to use, copy and modify."),
            ),
            parseLicense(notice, markdown = true),
        )
    }

    @Test
    fun `plain text keeps its line breaks and treats hashes as text`() {
        val licence = "MIT License\n\n# not a heading\nPermission is hereby granted,\nfree of charge."

        assertEquals(
            listOf(
                LicenseBlock.Paragraph("MIT License"),
                LicenseBlock.Paragraph("# not a heading\nPermission is hereby granted,\nfree of charge."),
            ),
            parseLicense(licence, markdown = false),
        )
    }
}
