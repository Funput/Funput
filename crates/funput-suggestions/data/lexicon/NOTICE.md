# Third-party data in the English lexicon

`en.tsv`, and the `en.lex` built from it, are derived from the sources below.
Anything that ships the lexicon must show this notice, for example on a
third-party licences screen.

## SCOWL / English Speller Database (ESDB)

Which words the lexicon may offer, and how each is spelled, come from the ESDB
word list of size 60, American spelling, variant level 1, at commit
`1e5b7d3a72f47a71da5d28686c1dd4b397178485` of <https://github.com/en-wl/wordlist>.
Funput keeps only entries made of plain ASCII letters, two to 32 of them.

```text
Copyright 2000-2026 by Kevin Atkinson

Permission to use, copy, modify, distribute, and sell any part of the English
Speller Database (ESDB, previously known as SCOWLv2), or word lists
created from it, is hereby granted without fee, provided that the above
copyright notice appears in all copies and that both the above copyright
notice and this notice appear in supporting documentation.  Kevin Atkinson
makes no representations about the suitability of this database for any
purpose.  It is provided "as is" without express or implied warranty.
```

A size-60 American word list needs no notice beyond the one above, according
to the ESDB `Copyright` file.

## Google Books Ngram Viewer

The order of the words comes from the Google Books Ngram Viewer exports,
version 20200217, English 1-grams, <http://books.google.com/ngrams>, licensed
under [Creative Commons Attribution 3.0 Unported](https://creativecommons.org/licenses/by/3.0/).
Funput sums each word's match counts over the years 2000–2019 across its
spellings and keeps only the resulting order; no counts are shipped.

## List of Dirty, Naughty, Obscene, and Otherwise Bad Words

The single-word entries of `blocklist.txt` up to its "Funput re-admits" line
come from the `en` list of
<https://github.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words>
at commit `5faf2ba42d7b1c0977169ec3611df25a3c08eb13`, by its contributors,
licensed under [Creative Commons Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/).
Funput dropped the multi-word phrases. The list is used only to remove words;
none of it is shipped.

## Funput

`supplement.tsv`, the re-admits and additions at the end of `blocklist.txt`, and the tools
that build the lexicon are Funput's own, under the MIT licence of this
repository.
