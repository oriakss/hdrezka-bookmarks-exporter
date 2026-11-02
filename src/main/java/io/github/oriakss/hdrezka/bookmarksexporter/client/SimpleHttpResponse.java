package io.github.oriakss.hdrezka.bookmarksexporter.client;

import java.net.URI;
import java.net.http.HttpHeaders;

public record SimpleHttpResponse(
        int statusCode,
        URI uri,
        String body,
        HttpHeaders headers
) {
}