package io.github.oriakss.hdrezka.bookmarksexporter.omdb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

public class OmdbClient {

    private static final String API_URL = "https://www.omdbapi.com/";
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OmdbClient(Path apiKeyPath) throws IOException {
        this.apiKey = Files.readString(apiKeyPath).trim();

        if (apiKey.isEmpty()) {
            throw new IllegalArgumentException("OMDb API key is empty");
        }

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        this.objectMapper = new ObjectMapper();
    }

    public Optional<OmdbMovie> findByImdbId(String imdbId)
            throws IOException, InterruptedException {
        if (imdbId == null || !imdbId.matches("tt\\d+")) {
            return Optional.empty();
        }

        return request("i", imdbId);
    }

    public Optional<OmdbMovie> findByTitle(
            String title,
            String year,
            String type
    ) throws IOException, InterruptedException {

        StringBuilder query = new StringBuilder();

        query.append("t=").append(encode(title));

        if (year != null && !year.isBlank()) {
            query.append("&y=").append(encode(year));
        }

        if (type != null && !type.isBlank()) {
            query.append("&type=").append(encode(type));
        }

        return request(query.toString());
    }

    private Optional<OmdbMovie> request(String parameter, String value)
            throws IOException, InterruptedException {
        return request(
                parameter + "=" + encode(value)
        );
    }

    private Optional<OmdbMovie> request(String query)
            throws IOException, InterruptedException {
        String url = API_URL
                + "?apikey=" + encode(apiKey)
                + "&" + query
                + "&plot=short&r=json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        JsonNode json = objectMapper.readTree(response.body());

        if (response.statusCode() != 200) {
            String error = json.path("Error")
                    .asText("Unknown error");

            throw new IOException(
                    "OMDb HTTP " + response.statusCode() + ": " + error
            );
        }

        if (!"True".equalsIgnoreCase(
                json.path("Response").asText())) {

            return Optional.empty();
        }

        String posterUrl = json.path("Poster").asText("");

        if (posterUrl.isBlank()
                || posterUrl.equalsIgnoreCase("N/A")) {
            posterUrl = "";
        }

        return Optional.of(new OmdbMovie(
                json.path("Title").asText(""),
                json.path("Year").asText(""),
                json.path("imdbID").asText(""),
                json.path("Type").asText(""),
                posterUrl
        ));
    }

    private String encode(String value) {
        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}