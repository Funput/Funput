package app.funput.funput.ui.news

/** A small feed in the shape funput.app serves, for the news tests. */
internal object NewsFixture {
    const val Feed = """
        {
          "version": "https://jsonfeed.org/version/1.1",
          "items": [
            {
              "id": "old-linux",
              "url": "https://funput.app/blog/old-linux/",
              "title": "Bộ gõ cho Linux",
              "summary": "Funput trên Linux.",
              "date_published": "2026-09-29T01:05:00Z",
              "tags": ["linux"]
            },
            {
              "id": "new-mac",
              "url": "https://funput.app/blog/new-mac/",
              "title": "Cài Funput trên macOS",
              "date_published": "2026-10-06T00:00:00.000Z",
              "tags": ["macos", "huong-dan"]
            },
            { "id": "broken", "url": "https://funput.app/blog/broken/", "title": "Thiếu ngày" },
            "not an item"
          ]
        }
    """
}
