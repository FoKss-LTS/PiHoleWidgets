package services.pihole;

import domain.configuration.DnsBlockerConfig;
import helpers.HttpClientUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import static org.junit.jupiter.api.Assertions.*;

class PiHoleHandlerTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getPiHoleStatsCallsSummaryEndpointWithSidAndReturnsRawJson() {
        AtomicReference<String> queryRef = new AtomicReference<>("");
        server.createContext("/api/stats/summary", exchange -> {
            queryRef.set(exchange.getRequestURI().getQuery());
            respondJson(exchange, 200, "{\"queries\":{},\"gravity\":{\"last_update\":0},\"took\":0.001}");
        });

        PiHoleHandler handler = new PiHoleHandler(
                DnsBlockerConfig.forPiHole("localhost", port, "http", "pw"),
                new HttpClientUtil(),
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                false,
                false);
        handler.setSessionId("SID123");

        String json = handler.getPiHoleStats();

        assertNotNull(json);
        assertTrue(json.contains("\"queries\""));
        assertNotNull(queryRef.get());
        assertTrue(queryRef.get().contains("sid=SID123"));
    }

    @Test
    void getLastBlockedCallsRecentBlockedAndReturnsFirstDomain() {
        AtomicReference<URI> uriRef = new AtomicReference<>();
        server.createContext("/api/stats/recent_blocked", exchange -> {
            uriRef.set(exchange.getRequestURI());
            respondJson(exchange, 200, "{\"blocked\":[\"doubleclick.net\"],\"took\":0.001}");
        });

        PiHoleHandler handler = new PiHoleHandler(
                DnsBlockerConfig.forPiHole("localhost", port, "http", "pw"),
                new HttpClientUtil(),
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                false,
                false);
        handler.setSessionId("abc");

        String lastBlocked = handler.getLastBlocked();

        assertEquals("doubleclick.net", lastBlocked);
        assertNotNull(uriRef.get());
        String query = uriRef.get().getQuery();
        assertNotNull(query);
        assertTrue(query.contains("sid=abc"));
        assertTrue(query.contains("count=1"));
    }

    @Test
    void getTopXBlockedCallsTopDomainsWithBlockedTrueAndCount() {
        AtomicReference<URI> uriRef = new AtomicReference<>();
        server.createContext("/api/stats/top_domains", exchange -> {
            uriRef.set(exchange.getRequestURI());
            respondJson(exchange, 200,
                    "{\"domains\":[{\"domain\":\"a.com\",\"count\":10}],\"blocked_queries\":10,\"total_queries\":100,\"took\":0.001}");
        });

        PiHoleHandler handler = new PiHoleHandler(
                DnsBlockerConfig.forPiHole("localhost", port, "http", "pw"),
                new HttpClientUtil(),
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                false,
                false);
        handler.setSessionId("SID");

        String json = handler.getTopXBlocked(5);

        assertNotNull(json);
        assertTrue(json.contains("\"domains\""));
        assertNotNull(uriRef.get());
        String query = uriRef.get().getQuery();
        assertNotNull(query);
        assertTrue(query.contains("sid=SID"));
        assertTrue(query.contains("blocked=true"));
        assertTrue(query.contains("count=5"));
    }

    @Test
    void getGravityLastUpdateFormatsRelativeTimeFromSummary() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        long lastUpdate = Instant.parse("2024-12-31T23:00:00Z").getEpochSecond();

        server.createContext("/api/stats/summary", exchange -> respondJson(exchange, 200,
                "{\"gravity\":{\"last_update\":" + lastUpdate + "},\"took\":0.001}"));

        PiHoleHandler handler = new PiHoleHandler(
                DnsBlockerConfig.forPiHole("localhost", port, "http", "pw"),
                new HttpClientUtil(),
                Clock.fixed(now, ZoneOffset.UTC),
                false,
                false);
        handler.setSessionId("S");

        String formatted = handler.getGravityLastUpdate();

        assertEquals("Gravity: 1h 0m ago", formatted);
    }

    @Test
    void getPiHoleStatsOmitsSidWhenSessionIsMissing() {
        AtomicReference<String> queryRef = new AtomicReference<>();
        server.createContext("/api/stats/summary", exchange -> {
            queryRef.set(exchange.getRequestURI().getQuery());
            respondJson(exchange, 200, "{\"queries\":{},\"took\":0.001}");
        });

        PiHoleHandler handler = new PiHoleHandler(
                DnsBlockerConfig.forPiHole("localhost", port, "http", ""),
                new HttpClientUtil(),
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                false,
                false);

        String json = handler.getPiHoleStats();

        assertNotNull(json);
        assertTrue(json.contains("\"queries\""));
        // No query string at all when sid is missing and no extra params were added
        assertNull(queryRef.get());
    }

    @Test
    void setDnsBlockingPostsJsonBodyAndSendsSidInQueryAndHeader() {
        AtomicReference<String> methodRef = new AtomicReference<>("");
        AtomicReference<String> queryRef = new AtomicReference<>("");
        AtomicReference<String> bodyRef = new AtomicReference<>("");
        AtomicReference<List<String>> headerSidRef = new AtomicReference<>();

        server.createContext("/api/dns/blocking", exchange -> {
            methodRef.set(exchange.getRequestMethod());
            queryRef.set(exchange.getRequestURI().getQuery());
            headerSidRef.set(exchange.getRequestHeaders().get("X-FTL-SID"));
            bodyRef.set(readAll(exchange.getRequestBody()));
            respondJson(exchange, 200, "{\"blocking\":\"disabled\",\"timer\":60,\"took\":0.001}");
        });

        PiHoleHandler handler = new PiHoleHandler(
                DnsBlockerConfig.forPiHole("localhost", port, "http", "pw"),
                new HttpClientUtil(),
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                false,
                false);
        handler.setSessionId("SID123");

        String json = handler.setDnsBlocking(false, 60);

        assertNotNull(json);
        assertTrue(json.contains("\"blocking\""));
        assertEquals("POST", methodRef.get());
        assertNotNull(queryRef.get());
        assertTrue(queryRef.get().contains("sid=SID123"));
        assertNotNull(headerSidRef.get());
        assertTrue(headerSidRef.get().contains("SID123"));
        assertNotNull(bodyRef.get());
        assertTrue(bodyRef.get().contains("\"blocking\":false"));
        assertTrue(bodyRef.get().contains("\"timer\":60"));
    }

    @Test
    void getDnsBlockingStatusCallsEndpointWithSidAndReturnsRawJson() {
        AtomicReference<URI> uriRef = new AtomicReference<>();
        server.createContext("/api/dns/blocking", exchange -> {
            uriRef.set(exchange.getRequestURI());
            respondJson(exchange, 200, "{\"blocking\":\"enabled\",\"took\":0.001}");
        });

        PiHoleHandler handler = new PiHoleHandler(
                DnsBlockerConfig.forPiHole("localhost", port, "http", "pw"),
                new HttpClientUtil(),
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                false,
                false);
        handler.setSessionId("SIDXYZ");

        String json = handler.getDnsBlockingStatus();

        assertNotNull(json);
        assertTrue(json.contains("\"blocking\""));
        assertNotNull(uriRef.get());
        String query = uriRef.get().getQuery();
        assertNotNull(query);
        assertTrue(query.contains("sid=SIDXYZ"));
    }

    private void respondJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body == null ? new byte[0] : body.getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        } finally {
            exchange.close();
        }
    }

    private String readAll(InputStream is) throws IOException {
        if (is == null)
            return "";
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
}
