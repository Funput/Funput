package app.funput.funput.ime.suggestions

import android.content.Context
import app.funput.funput.ime.suggestions.lexicon.LexiconInstaller

internal fun lexiconInstaller(context: Context) = LexiconInstaller(
    source = { context.assets.open("lexicon/en.lex") },
    directory = { context.noBackupFilesDir.resolve("Lexicon") },
)

/** The personal store: opened by the keyboard, read by the settings app through a snapshot. */
internal fun personalSuggestionDirectory(context: Context) = context.noBackupFilesDir.resolve("PersonalSuggestions")

internal fun suggestionWorker(context: Context, publish: (PersonalSuggestionRequest, List<String>) -> Unit) =
    PersonalSuggestionWorker(
        storeDirectory = { personalSuggestionDirectory(context) },
        attachLexicon = { engine -> lexiconInstaller(context).attach(engine::attachLexicon) },
        publish = publish,
    )
