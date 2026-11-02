package io.github.oriakss.hdrezka.bookmarksexporter.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.zip.GZIPInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HDrezkaClient {

    private final HttpClient httpClient;
    private final String cookies;

    public HDrezkaClient(Path cookiesPath) throws IOException {
        this.cookies = Files.readString(cookiesPath).trim();

        if (cookies.isEmpty()) {
            throw new IllegalArgumentException("Cookies file is empty");
        }

        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public SimpleHttpResponse get(String url) throws IOException, InterruptedException {
        URI uri = URI.create(url);
        String host = uri.getHost();

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || host == null
                || !(host.equals("rezka-ua.tv")
                || host.endsWith(".rezka-ua.tv"))) {
            throw new IllegalArgumentException(
                    "Only rezka-ua.tv HTTPS URLs are allowed"
            );
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(30))
                .header("Cookie", cookies)
                .header("Accept-Encoding", "gzip")
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                                + "Chrome/130.0.0.0 Safari/537.36")
                .header("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8")
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        byte[] body = response.body();

        String encoding = response.headers()
                .firstValue("Content-Encoding")
                .orElse("");

        if (encoding.toLowerCase().contains("gzip")) {
            try (GZIPInputStream gzip = new GZIPInputStream(
                    new ByteArrayInputStream(body))) {
                body = gzip.readAllBytes();
            }
        }

        String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("");

        Charset charset = StandardCharsets.UTF_8;

        Matcher matcher = Pattern.compile("(?i)charset\\s*=\\s*[\"']?([^;\"'\\s]+)")
                .matcher(contentType);

        if (matcher.find()) {
            try {
                charset = Charset.forName(matcher.group(1));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return new SimpleHttpResponse(
                response.statusCode(),
                response.uri(),
                new String(body, charset),
                response.headers()
        );
    }
}