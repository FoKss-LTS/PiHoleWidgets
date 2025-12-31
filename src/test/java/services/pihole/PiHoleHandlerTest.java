package services.pihole;

import domain.configuration.DnsBlockerConfig;
import helpers.HttpClientUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

    // ==================== Session Expiry and Retry Tests ====================

    @Nested
    @DisplayName("Session Expiry and Automatic Retry")
    class SessionExpiryTests {

        @Test
        @DisplayName("GET request should retry after 401 with re-authentication")
        void getRequestShouldRetryAfter401() {
            AtomicInteger summaryCallCount = new AtomicInteger(0);
            AtomicInteger authCallCount = new AtomicInteger(0);

            // Auth endpoint
            server.createContext("/api/auth", exchange -> {
                authCallCount.incrementAndGet();
                respondJson(exchange, 200,
                    "{\"session\":{\"valid\":true,\"sid\":\"NEW_SESSION_ID\"},\"took\":0.001}");
            });

            // Summary endpoint - first call returns 401, second succeeds
            server.createContext("/api/stats/summary", exchange -> {
                int callNum = summaryCallCount.incrementAndGet();
                if (callNum == 1) {
                    // First call - session expired
                    respondJson(exchange, 401, "{\"error\":\"unauthorized\"}");
                } else {
                    // Second call (after re-auth) - success
                    respondJson(exchange, 200, "{\"queries\":{\"total\":100},\"took\":0.001}");
                }
            });

            PiHoleHandler handler = new PiHoleHandler(
                    DnsBlockerConfig.forPiHole("localhost", port, "http", "password123"),
                    new HttpClientUtil(),
                    Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                    false,
                    false);
            handler.setSessionId("OLD_EXPIRED_SESSION");

            String json = handler.getPiHoleStats();

            assertNotNull(json);
            assertTrue(json.contains("\"queries\""));
            assertEquals(2, summaryCallCount.get(), "Summary should be called twice (initial + retry)");
            assertEquals(1, authCallCount.get(), "Auth should be called once for re-authentication");
            assertEquals("NEW_SESSION_ID", handler.getSessionId(), "Session ID should be updated");
        }

        @Test
        @DisplayName("GET request should retry after 403 with re-authentication")
        void getRequestShouldRetryAfter403() {
            AtomicInteger summaryCallCount = new AtomicInteger(0);
            AtomicInteger authCallCount = new AtomicInteger(0);

            server.createContext("/api/auth", exchange -> {
                authCallCount.incrementAndGet();
                respondJson(exchange, 200,
                    "{\"session\":{\"valid\":true,\"sid\":\"REFRESHED_SID\"},\"took\":0.001}");
            });

            server.createContext("/api/stats/summary", exchange -> {
                int callNum = summaryCallCount.incrementAndGet();
                if (callNum == 1) {
                    respondJson(exchange, 403, "{\"error\":\"forbidden\"}");
                } else {
                    respondJson(exchange, 200, "{\"queries\":{\"blocked\":50},\"took\":0.001}");
                }
            });

            PiHoleHandler handler = new PiHoleHandler(
                    DnsBlockerConfig.forPiHole("localhost", port, "http", "mypassword"),
                    new HttpClientUtil(),
                    Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                    false,
                    false);
            handler.setSessionId("STALE_SESSION");

            String json = handler.getPiHoleStats();

            assertNotNull(json);
            assertEquals(2, summaryCallCount.get());
            assertEquals(1, authCallCount.get());
            assertEquals("REFRESHED_SID", handler.getSessionId());
        }

        @Test
        @DisplayName("POST request should retry after 401 with re-authentication")
        void postRequestShouldRetryAfter401() {
            AtomicInteger blockingCallCount = new AtomicInteger(0);
            AtomicInteger authCallCount = new AtomicInteger(0);

            server.createContext("/api/auth", exchange -> {
                authCallCount.incrementAndGet();
                respondJson(exchange, 200,
                    "{\"session\":{\"valid\":true,\"sid\":\"POST_NEW_SID\"},\"took\":0.001}");
            });

            server.createContext("/api/dns/blocking", exchange -> {
                int callNum = blockingCallCount.incrementAndGet();
                if (callNum == 1) {
                    respondJson(exchange, 401, "{\"error\":\"unauthorized\"}");
                } else {
                    respondJson(exchange, 200, "{\"blocking\":\"disabled\",\"took\":0.001}");
                }
            });

            PiHoleHandler handler = new PiHoleHandler(
                    DnsBlockerConfig.forPiHole("localhost", port, "http", "pass"),
                    new HttpClientUtil(),
                    Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                    false,
                    false);
            handler.setSessionId("EXPIRED_POST_SESSION");

            String json = handler.setDnsBlocking(false, null);

            assertNotNull(json);
            assertTrue(json.contains("\"blocking\""));
            assertEquals(2, blockingCallCount.get());
            assertEquals(1, authCallCount.get());
            assertEquals("POST_NEW_SID", handler.getSessionId());
        }

        @Test
        @DisplayName("Should not retry without password configured")
        void shouldNotRetryWithoutPassword() {
            AtomicInteger summaryCallCount = new AtomicInteger(0);
            AtomicInteger authCallCount = new AtomicInteger(0);

            server.createContext("/api/auth", exchange -> {
                authCallCount.incrementAndGet();
                respondJson(exchange, 200, "{\"session\":{\"valid\":true,\"sid\":\"X\"},\"took\":0.001}");
            });

            server.createContext("/api/stats/summary", exchange -> {
                summaryCallCount.incrementAndGet();
                respondJson(exchange, 401, "{\"error\":\"unauthorized\"}");
            });

            // No password configured
            PiHoleHandler handler = new PiHoleHandler(
                    DnsBlockerConfig.forPiHole("localhost", port, "http", ""),
                    new HttpClientUtil(),
                    Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                    false,
                    false);
            handler.setSessionId("SOME_SID");

            String json = handler.getPiHoleStats();

            // Should return empty because it can't retry without password
            assertEquals("", json);
            assertEquals(1, summaryCallCount.get(), "Should only call once without retry");
            assertEquals(0, authCallCount.get(), "Should not attempt auth without password");
        }

        @Test
        @DisplayName("Authentication failure should clear session ID")
        void authenticationFailureShouldClearSessionId() {
            server.createContext("/api/auth", exchange -> {
                respondJson(exchange, 401, "{\"error\":\"invalid password\"}");
            });

            PiHoleHandler handler = new PiHoleHandler(
                    DnsBlockerConfig.forPiHole("localhost", port, "http", "wrongpassword"),
                    new HttpClientUtil(),
                    Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                    false,
                    false);
            handler.setSessionId("PRE_EXISTING_SESSION");

            boolean result = handler.authenticate();

            assertFalse(result, "Authentication should fail");
            assertNull(handler.getSessionId(), "Session ID should be cleared on auth failure");
        }

        @Test
        @DisplayName("Successful authentication should set new session ID")
        void successfulAuthenticationShouldSetSessionId() {
            server.createContext("/api/auth", exchange -> {
                respondJson(exchange, 200,
                    "{\"session\":{\"valid\":true,\"sid\":\"BRAND_NEW_SESSION\",\"message\":\"ok\"},\"took\":0.001}");
            });

            PiHoleHandler handler = new PiHoleHandler(
                    DnsBlockerConfig.forPiHole("localhost", port, "http", "correctpassword"),
                    new HttpClientUtil(),
                    Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                    false,
                    false);

            boolean result = handler.authenticate();

            assertTrue(result, "Authentication should succeed");
            assertEquals("BRAND_NEW_SESSION", handler.getSessionId());
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Concurrent authentication requests should be serialized")
        void concurrentAuthenticationShouldBeSerialized() throws InterruptedException {
            AtomicInteger authCallCount = new AtomicInteger(0);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(3);

            server.createContext("/api/auth", exchange -> {
                authCallCount.incrementAndGet();
                // Simulate slow auth
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                respondJson(exchange, 200,
                    "{\"session\":{\"valid\":true,\"sid\":\"CONCURRENT_SID\"},\"took\":0.001}");
            });

            PiHoleHandler handler = new PiHoleHandler(
                    DnsBlockerConfig.forPiHole("localhost", port, "http", "password"),
                    new HttpClientUtil(),
                    Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                    false,
                    false);

            // Start 3 threads that all try to authenticate at once
            for (int i = 0; i < 3; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        handler.authenticate();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            // Release all threads at once
            startLatch.countDown();

            // Wait for all to complete
            assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should complete");

            // Due to synchronized block, auth should be called 3 times (each thread acquires lock)
            // but the session ID should be consistently set
            assertEquals("CONCURRENT_SID", handler.getSessionId());
        }
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
