/*
 *
 *  Copyright (C) 2022 - 2025.  Reda ELFARISSI aka FoKss-LTS
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Small HTTP utility tailored for HTTPS/HTTP requests.
 * Provides helpers for common verbs, status/introspection, and JSON parsing.
 */
public class HttpClientUtil {

    private static final Logger LOGGER = Logger.getLogger(HttpClientUtil.class.getName());
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);

    // Enable verbose logging via system property: -Dpihole.verbose=true
    public static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("pihole.verbose", "false"));

    private final HttpClient client;
    private final Duration defaultRequestTimeout;
    private final ObjectMapper mapper;

    private static void log(String message) {
        if (VERBOSE) {
            LOGGER.log(Level.FINE, () -> "[HTTP] " + message);
        }
    }

    public HttpClientUtil() {
        this(DEFAULT_CONNECT_TIMEOUT, DEFAULT_REQUEST_TIMEOUT, new ObjectMapper());
    }

    public HttpClientUtil(Duration connectTimeout, Duration requestTimeout) {
        this(connectTimeout, requestTimeout, new ObjectMapper());
    }

    public HttpClientUtil(Duration connectTimeout, Duration requestTimeout, ObjectMapper mapper) {
        Duration safeConnectTimeout = connectTimeout == null ? DEFAULT_CONNECT_TIMEOUT : connectTimeout;
        Duration safeRequestTimeout = requestTimeout == null ? DEFAULT_REQUEST_TIMEOUT : requestTimeout;
        this.client = HttpClient.newBuilder()
                .connectTimeout(safeConnectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.defaultRequestTimeout = safeRequestTimeout;
        this.mapper = mapper == null ? new ObjectMapper() : mapper;
    }

    public HttpResponsePayload get(String url) throws IOException, InterruptedException {
        return send(url, HttpMethod.GET, Collections.emptyMap(), null, Collections.emptyMap(), null);
    }

    public HttpResponsePayload get(String url, Map<String, String> queryParams, Map<String, String> headers)
            throws IOException, InterruptedException {
        return send(url, HttpMethod.GET, headers, null, queryParams, null);
    }

    public HttpResponsePayload delete(String url, Map<String, String> headers)
            throws IOException, InterruptedException {
        return send(url, HttpMethod.DELETE, headers, null, Collections.emptyMap(), null);
    }

    public HttpResponsePayload post(String url, String body, Map<String, String> headers)
            throws IOException, InterruptedException {
        return send(url, HttpMethod.POST, headers, body, Collections.emptyMap(), null);
    }

    public HttpResponsePayload put(String url, String body, Map<String, String> headers)
            throws IOException, InterruptedException {
        return send(url, HttpMethod.PUT, headers, body, Collections.emptyMap(), null);
    }

    public HttpResponsePayload postJson(String url, Object body, Map<String, String> headers)
            throws IOException, InterruptedException {
        Map<String, String> mergedHeaders = new HashMap<>();
        if (headers != null)
            mergedHeaders.putAll(headers);
        mergedHeaders.putIfAbsent("Content-Type", "application/json");
        mergedHeaders.putIfAbsent("Accept", "application/json");
        String json = body == null ? "" : mapper.writeValueAsString(body);
        return send(url, HttpMethod.POST, mergedHeaders, json, Collections.emptyMap(), null);
    }

    public HttpResponsePayload postJson(String url,
            Object body,
            Map<String, String> queryParams,
            Map<String, String> headers) throws IOException, InterruptedException {
        Map<String, String> mergedHeaders = new HashMap<>();
        if (headers != null)
            mergedHeaders.putAll(headers);
        mergedHeaders.putIfAbsent("Content-Type", "application/json");
        mergedHeaders.putIfAbsent("Accept", "application/json");
        String json = body == null ? "" : mapper.writeValueAsString(body);
        return send(url, HttpMethod.POST, mergedHeaders, json,
                queryParams == null ? Collections.emptyMap() : queryParams, null);
    }

    public HttpResponsePayload send(String url,
            HttpMethod method,
            Map<String, String> headers,
            String body,
            Map<String, String> queryParams,
            Duration timeout) throws IOException, InterruptedException {
        URI uri = buildUri(url, queryParams);

        log(">>> " + method + " " + uri);
        if (headers != null && !headers.isEmpty()) {
            log("    Headers: " + headers);
        }
        if (body != null && !body.isBlank()) {
            // Mask password in logs
            String safeBody = body.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"");
            log("    Body: " + safeBody);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(timeout == null ? defaultRequestTimeout : timeout);

        applyHeaders(builder, headers);
        builder.method(method.name(), buildBodyPublisher(method, body));

        long startTime = System.currentTimeMillis();
        HttpResponse<String> response = client.send(builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        long duration = System.currentTimeMillis() - startTime;

        log("<<< " + method + " " + uri + " -> " + response.statusCode() + " (" + duration + "ms)");
        if (VERBOSE && response.body() != null) {
            String responseBody = response.body();
            // Truncate very long responses
            if (responseBody.length() > 1000) {
                log("    Response (truncated): " + responseBody.substring(0, 1000) + "...");
            } else {
                log("    Response: " + responseBody);
            }
        }

        return new HttpResponsePayload(method, uri, response, mapper);
    }

    private void applyHeaders(HttpRequest.Builder builder, Map<String, String> headers) {
        if (headers == null || headers.isEmpty())
            return;
        headers.forEach(builder::header);
    }

    private HttpRequest.BodyPublisher buildBodyPublisher(HttpMethod method, String body) {
        boolean shouldSendBody = method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
        if (!shouldSendBody)
            return HttpRequest.BodyPublishers.noBody();
        if (body == null || body.isBlank())
            return HttpRequest.BodyPublishers.noBody();
        return HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
    }

    private URI buildUri(String baseUrl, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty())
            return URI.create(baseUrl);
        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        if (!baseUrl.contains("?")) {
            urlBuilder.append("?");
        } else if (!baseUrl.endsWith("&") && !baseUrl.endsWith("?")) {
            urlBuilder.append("&");
        }
        queryParams.forEach((key, value) -> {
            if (urlBuilder.charAt(urlBuilder.length() - 1) != '?'
                    && urlBuilder.charAt(urlBuilder.length() - 1) != '&') {
                urlBuilder.append("&");
            }
            urlBuilder.append(encode(key)).append("=").append(encode(value));
        });
        return URI.create(urlBuilder.toString());
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    public enum HttpMethod {
        GET, POST, PUT, DELETE, PATCH
    }

    /**
     * Simple response wrapper exposing status, headers, body as text/JSON.
     */
    public static final class HttpResponsePayload {
        private final HttpMethod method;
        private final URI uri;
        private final int statusCode;
        private final HttpHeaders headers;
        private final String body;
        private final ObjectMapper mapper;

        private HttpResponsePayload(HttpMethod method, URI uri, HttpResponse<String> response, ObjectMapper mapper) {
            this.method = method;
            this.uri = uri;
            this.statusCode = response.statusCode();
            this.headers = response.headers();
            this.body = response.body();
            this.mapper = mapper;
        }

        public HttpMethod method() {
            return method;
        }

        public URI uri() {
            return uri;
        }

        public int statusCode() {
            return statusCode;
        }

        public HttpHeaders headers() {
            return headers;
        }

        public String bodyText() {
            return body;
        }

        public boolean isSuccessful() {
            return statusCode >= 200 && statusCode < 300;
        }

        public Optional<JsonNode> bodyAsJson() {
            if (body == null || body.isBlank())
                return Optional.empty();
            try {
                return Optional.of(mapper.readTree(body));
            } catch (JsonProcessingException e) {
                return Optional.empty();
            }
        }
    }
}
