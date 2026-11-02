package io.github.oriakss.hdrezka.bookmarksexporter;

import io.github.oriakss.hdrezka.bookmarksexporter.client.HDrezkaClient;
import io.github.oriakss.hdrezka.bookmarksexporter.omdb.BookmarkEnricher;
import io.github.oriakss.hdrezka.bookmarksexporter.omdb.OmdbClient;
import io.github.oriakss.hdrezka.bookmarksexporter.parser.Bookmark;
import io.github.oriakss.hdrezka.bookmarksexporter.parser.BookmarkCrawler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            HDrezkaClient client = new HDrezkaClient(Path.of("cookies.txt"));

            BookmarkCrawler crawler = new BookmarkCrawler(client);

            List<Bookmark> bookmarks = crawler.crawl("https://rezka-ua.tv/favorites/97769/");

            OmdbClient omdbClient = new OmdbClient(Path.of("omdb-api-key.txt"));

            BookmarkEnricher enricher = new BookmarkEnricher(client, omdbClient);

            List<Bookmark> enriched = enricher.enrich(bookmarks);

            saveCsv(enriched, Path.of("export/bookmarks.csv"));

            System.out.println("--------------------------------");
            System.out.println("Export completed");
            System.out.println("Output: export/bookmarks.csv");

        } catch (Exception e) {
            logger.error("Export failed", e);
        }
    }

    private static void saveCsv(List<Bookmark> bookmarks, Path output) throws IOException {
        Files.createDirectories(output.getParent());

        List<String> lines = new ArrayList<>();

        lines.add("id,title,original_title,url,hdrezka_poster_url," + "type,description,imdb_id,omdb_poster_url");

        for (Bookmark bookmark : bookmarks) {

            lines.add(String.join(",",
                    csv(bookmark.id()),
                    csv(bookmark.title()),
                    csv(bookmark.originalTitle()),
                    csv(bookmark.url()),
                    csv(bookmark.posterUrl()),
                    csv(bookmark.type()),
                    csv(bookmark.description()),
                    csv(bookmark.imdbId()),
                    csv(bookmark.omdbPosterUrl())
            ));
        }

        String content = "\uFEFF" + String.join(System.lineSeparator(), lines);

        Files.writeString(output, content, StandardCharsets.UTF_8);
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}