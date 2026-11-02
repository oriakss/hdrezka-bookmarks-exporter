package io.github.oriakss.hdrezka.bookmarksexporter.omdb;

public record OmdbMovie(
        String title,
        String year,
        String imdbId,
        String type,
        String posterUrl
) {
}