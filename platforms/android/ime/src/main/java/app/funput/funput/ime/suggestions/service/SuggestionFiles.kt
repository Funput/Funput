package app.funput.funput.ime.suggestions

import android.content.Context
import app.funput.funput.ime.suggestions.lexicon.LexiconInstaller

internal fun lexiconInstaller(context: Context) = LexiconInstaller(
    source = { context.assets.open("lexicon/en.lex") },
    directory = { context.noBackupFilesDir.resolve("Lexicon") },
)

internal fun suggestionWorker(context: Context, publish: (PersonalSuggestionRequest, List<String>) -> Unit) =
    PersonalSuggestionWorker(
        storeDirectory = { context.noBackupFilesDir.resolve("PersonalSuggestions") },
        attachLexicon = { engine -> lexiconInstaller(context).attach(engine::attachLexicon) },
        publish = publish,
    )
