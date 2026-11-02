package io.github.oriakss.hdrezka.bookmarksexporter.omdb;

import io.github.oriakss.hdrezka.bookmarksexporter.client.HDrezkaClient;
import io.github.oriakss.hdrezka.bookmarksexporter.client.SimpleHttpResponse;
import io.github.oriakss.hdrezka.bookmarksexporter.parser.Bookmark;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BookmarkEnricher {

    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b");
    private static final Pattern IMDB_PATTERN = Pattern.compile("tt\\d+");
    private static final long REQUEST_DELAY_MS = 250;
    private final HDrezkaClient hdrezkaClient;
    private final OmdbClient omdbClient;

    public BookmarkEnricher(HDrezkaClient hdrezkaClient, OmdbClient omdbClient) {
        this.hdrezkaClient = hdrezkaClient;
        this.omdbClient = omdbClient;
    }

    public List<Bookmark> enrich(List<Bookmark> bookmarks) throws InterruptedException {
        List<Bookmark> enriched = new ArrayList<>();

        int found = 0;
        int notFound = 0;
        int skipped = 0;
        int errors = 0;

        for (int i = 0; i < bookmarks.size(); i++) {

            Bookmark bookmark = bookmarks.get(i);

            System.out.println(
                    "[" + (i + 1) + "/" + bookmarks.size() + "] "
                            + bookmark.title()
            );

            try {
                SimpleHttpResponse response = hdrezkaClient.get(bookmark.url());

                if (response.statusCode() != 200) {
                    System.err.println("Page request failed: HTTP " + response.statusCode());

                    enriched.add(bookmark);
                    errors++;
                    continue;
                }

                Document document = Jsoup.parse(
                        response.body(),
                        response.uri().toString()
                );

                String originalTitle = extractOriginalTitle(document);

                String imdbId = extractImdbId(document);

                if (originalTitle == null
                        || originalTitle.isBlank()) {
                    originalTitle = bookmark.title();
                }

                bookmark = bookmark.withOriginalTitle(originalTitle);

                if (imdbId != null) {
                    bookmark = bookmark.withImdbId(imdbId);
                }

                System.out.println("Original title: " + originalTitle);
                System.out.println("IMDb ID: " + imdbId);

                Optional<OmdbMovie> result = Optional.empty();

                if (imdbId != null) {
                    result = omdbClient.findByImdbId(imdbId);

                    if (result.isPresent()) {
                        System.out.println("Match method: IMDb ID");
                    }
                }

                if (result.isEmpty()) {
                    result = findByAlternativeTitles(
                            originalTitle,
                            bookmark.description()
                    );
                }

                if (result.isPresent()) {
                    OmdbMovie movie = result.get();

                    bookmark = bookmark.withOmdbData(
                            movie.imdbId(),
                            movie.posterUrl()
                    );

                    found++;

                    System.out.println("Matched: " + movie.title() + " | " + movie.imdbId());
                } else {
                    notFound++;
                    System.out.println("No match");
                }

            } catch (IOException e) {

                errors++;

                System.err.println(
                        "Processing failed: " + e.getMessage()
                );
            }

            enriched.add(bookmark);

            if (i < bookmarks.size() - 1) {
                TimeUnit.MILLISECONDS.sleep(REQUEST_DELAY_MS);
            }
        }

        System.out.println("--------------------------------");
        System.out.println("Total: " + bookmarks.size());
        System.out.println("Matched: " + found);
        System.out.println("Not found: " + notFound);
        System.out.println("Skipped: " + skipped);
        System.out.println("Errors: " + errors);

        return enriched;
    }

    private String extractOriginalTitle(Document document) {

        Element element = document.selectFirst(".b-post__origtitle");

        if (element == null) {
            return null;
        }

        String title = element.text().trim();

        return title.isBlank() ? null : title;
    }

    private String extractImdbId(Document document) {
        Element imdbLink = document.selectFirst(".b-post__info_rates.imdb a[href]");

        if (imdbLink == null) {
            return null;
        }

        String href = imdbLink.attr("href");

        String prefix = "/help/";

        if (!href.startsWith(prefix)) {
            return null;
        }

        String encoded = href.substring(prefix.length());

        int slashIndex = encoded.indexOf('/');

        if (slashIndex >= 0) {
            encoded = encoded.substring(0, slashIndex);
        }

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encoded);

            String decoded = new String(decodedBytes, StandardCharsets.UTF_8);

            String decodedUrl = URLDecoder.decode(decoded, StandardCharsets.UTF_8);

            Matcher matcher = IMDB_PATTERN.matcher(decodedUrl);

            return matcher.find() ? matcher.group() : null;

        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Optional<OmdbMovie> findByAlternativeTitles(String originalTitle, String description)
            throws IOException, InterruptedException {
        String year = extractYear(description);

        if (year == null) {
            return Optional.empty();
        }

        String[] titles = originalTitle.split("\\s*/\\s*");

        for (String title : titles) {

            title = title.trim();

            if (title.isBlank()) {
                continue;
            }

            System.out.println("Searching title: " + title);

            Optional<OmdbMovie> result = omdbClient.findByTitle(title, year, null);

            if (result.isPresent()) {

                OmdbMovie movie = result.get();

                if (yearMatches(year, movie.year())) {
                    return result;
                }

                System.out.println("Year mismatch");
            }
        }

        return Optional.empty();
    }

    private String extractYear(String description) {
        if (description == null) {
            return null;
        }

        Matcher matcher = YEAR_PATTERN.matcher(description);

        return matcher.find() ? matcher.group(1) : null;
    }

    private boolean yearMatches(String requested, String returned) {
        return returned != null && returned.contains(requested);
    }
}