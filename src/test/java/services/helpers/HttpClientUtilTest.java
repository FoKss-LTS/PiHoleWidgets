package services.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import helpers.HttpClientUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;


import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic HTTP client util tests using an in-memory HttpServer.
 */
class HttpClientUtilTest {

    private HttpServer server;
    private int port;
    private HttpClientUtil client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        port = server.getAddress().getPort();
        client = new HttpClientUtil();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getReturnsBodyAndStatus() throws Exception {
        server.createContext("/hello", exchange -> respond(exchange, 200, "hi there"));

        HttpClientUtil.HttpResponsePayload response = client.get(baseUrl("/hello"));

        assertTrue(response.isSuccessful());
        assertEquals(200, response.statusCode());
        assertEquals("hi there", response.bodyText());
    }

    @Test
    void postJsonSendsBodyAndParsesJsonResponse() throws Exception {
        server.createContext("/echo", new EchoJsonHandler());

        Map<String, Object> requestBody = Map.of("ping", "pong");
        HttpClientUtil.HttpResponsePayload response = client.postJson(baseUrl("/echo"), requestBody, new HashMap<>());

        assertTrue(response.isSuccessful());
        Optional<JsonNode> json = response.bodyAsJson();
        assertTrue(json.isPresent());
        assertEquals("pong", json.get().get("ping").asText());
        assertEquals("POST", response.headers().firstValue("X-Echo-Method").orElse(""));
    }

    @Test
    void getWithQueryParamsBuildsUrl() throws Exception {
        server.createContext("/withQuery", exchange -> {
            URI uri = exchange.getRequestURI();
            respond(exchange, 200, uri.getQuery());
        });

        Map<String, String> params = Map.of("a", "1", "b", "x y");
        HttpClientUtil.HttpResponsePayload response = client.get(baseUrl("/withQuery"), params, new HashMap<>());

        assertTrue(response.isSuccessful());
        String body = response.bodyText();
        assertTrue(body.contains("a=1"));
        assertTrue(body.contains("b=x+y") || body.contains("b=x%20y"));
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body == null ? new byte[0] : body.getBytes();
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        } finally {
            exchange.close();
        }
    }

    private class EchoJsonHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("X-Echo-Method", exchange.getRequestMethod());
            exchange.sendResponseHeaders(200, requestBody.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(requestBody);
            } finally {
                exchange.close();
            }
        }
    }
}

