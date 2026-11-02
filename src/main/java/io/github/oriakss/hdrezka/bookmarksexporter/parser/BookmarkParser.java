package io.github.oriakss.hdrezka.bookmarksexporter.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class BookmarkParser {

    public List<Bookmark> parse(String html) {
        Document document = Jsoup.parse(html);
        Elements cards = document.select("div.b-content__inline_item");

        List<Bookmark> bookmarks = new ArrayList<>();

        for (Element card : cards) {
            String id = card.attr("data-id");
            String url = card.attr("data-url");

            Element titleElement = card.selectFirst(".b-content__inline_item-link > a");

            Element posterElement = card.selectFirst(".b-content__inline_item-cover img");

            Element typeElement = card.selectFirst(".cat .entity");

            Element descriptionElement = card.selectFirst(".b-content__inline_item-link > div");

            String title = titleElement != null ? titleElement.text() : "";

            String posterUrl = posterElement != null ? posterElement.attr("src") : "";

            String type = typeElement != null ? typeElement.text() : "";

            String description = descriptionElement != null ? descriptionElement.text() : "";

            if (!id.isEmpty() && !url.isEmpty()) {
                bookmarks.add(new Bookmark(id, title, url, posterUrl, type, description));
            }
        }

        return bookmarks;
    }
}