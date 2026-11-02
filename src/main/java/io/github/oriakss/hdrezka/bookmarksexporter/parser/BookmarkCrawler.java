package io.github.oriakss.hdrezka.bookmarksexporter.parser;

import io.github.oriakss.hdrezka.bookmarksexporter.client.HDrezkaClient;
import io.github.oriakss.hdrezka.bookmarksexporter.client.SimpleHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BookmarkCrawler {

    private final HDrezkaClient client;
    private final BookmarkParser parser;

    public BookmarkCrawler(HDrezkaClient client) {
        this.client = client;
        this.parser = new BookmarkParser();
    }

    public List<Bookmark> crawl(String startUrl) throws IOException, InterruptedException {
        LinkedHashMap<String, Bookmark> allBookmarks = new LinkedHashMap<>();

        Set<String> visitedPages = new HashSet<>();

        String currentUrl = startUrl;
        int pageNumber = 1;

        while (currentUrl != null && !visitedPages.contains(currentUrl)) {
            visitedPages.add(currentUrl);

            System.out.println("Loading page " + pageNumber + ": " + currentUrl);

            SimpleHttpResponse response = client.get(currentUrl);

            if (response.statusCode() != 200) {
                throw new IOException("Error load page: HTTP " + response.statusCode());
            }

            Document document = Jsoup.parse(
                    response.body(),
                    response.uri().toString()
            );

            List<Bookmark> pageBookmarks = parser.parse(response.body());

            int added = 0;

            for (Bookmark bookmark : pageBookmarks) {
                if (!allBookmarks.containsKey(bookmark.id())) {
                    allBookmarks.put(bookmark.id(), bookmark);
                    added++;
                }
            }

            System.out.println("Found: " + pageBookmarks.size() + ", added new: " + added);

            Element nextLink = document.selectFirst("link[rel=next]");

            if (nextLink == null) {
                currentUrl = null;
            } else {
                currentUrl = nextLink.absUrl("href");

                if (currentUrl.isBlank()) {
                    currentUrl = null;
                }
            }

            pageNumber++;
        }

        List<Bookmark> result = new ArrayList<>(allBookmarks.values());

        System.out.println("--------------------------------");
        System.out.println("Total pages: " + (pageNumber - 1));
        System.out.println("Total unique bookmarks: " + result.size());

        return result;
    }
}