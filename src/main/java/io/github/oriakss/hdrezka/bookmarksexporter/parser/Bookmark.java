package io.github.oriakss.hdrezka.bookmarksexporter.parser;

public record Bookmark(
        String id,
        String title,
        String url,
        String posterUrl,
        String type,
        String description,
        String originalTitle,
        String imdbId,
        String omdbPosterUrl
) {
    public Bookmark(
            String id,
            String title,
            String url,
            String posterUrl,
            String type,
            String description
    ) {
        this(
                id, title, url, posterUrl, type, description,
                null, null, null
        );
    }

    public Bookmark withOriginalTitle(String originalTitle) {
        return new Bookmark(
                id, title, url, posterUrl, type, description,
                originalTitle, imdbId, omdbPosterUrl
        );
    }

    public Bookmark withImdbId(String imdbId) {
        return new Bookmark(
                id, title, url, posterUrl, type, description,
                originalTitle, imdbId, omdbPosterUrl
        );
    }

    public Bookmark withOmdbData(String imdbId, String omdbPosterUrl) {
        return new Bookmark(
                id, title, url, posterUrl, type, description,
                originalTitle, imdbId, omdbPosterUrl
        );
    }
}