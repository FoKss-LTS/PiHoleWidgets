package services.adguard;

import domain.configuration.DnsBlockerConfig;
import domain.configuration.DnsBlockerType;
import helpers.HttpClientUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AdGuardHomeHandler.
 * 
 * Tests focus on:
 * - Basic Auth header encoding (especially UTF-8 for non-ASCII characters)
 * - API endpoint calls
 * - Response parsing and transformation to generic schema
 */
class AdGuardHomeHandlerTest {

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

    // ==================== Basic Auth Encoding Tests ====================

    @Nested
    @DisplayName("Basic Authentication Header Encoding")
    class BasicAuthEncodingTests {

        @Test
        @DisplayName("Should encode ASCII credentials correctly")
        void shouldEncodeAsciiCredentialsCorrectly() {
            AtomicReference<String> authHeaderRef = new AtomicReference<>();

            server.createContext("/control/status", exchange -> {
                List<String> authHeaders = exchange.getRequestHeaders().get("Authorization");
                if (authHeaders != null && !authHeaders.isEmpty()) {
                    authHeaderRef.set(authHeaders.get(0));
                }
                respondJson(exchange, 200, "{\"version\":\"0.107.0\",\"protection_enabled\":true}");
            });

            DnsBlockerConfig config = new DnsBlockerConfig(
                    DnsBlockerType.ADGUARD_HOME, "localhost", port, "http", "admin", "password123");
            AdGuardHomeHandler handler = new AdGuardHomeHandler(config, new HttpClientUtil());

            handler.getVersion();

            assertNotNull(authHeaderRef.get());
            assertTrue(authHeaderRef.get().startsWith("Basic "));

            // Verify the encoded credentials
            String encoded = authHeaderRef.get().substring("Basic ".length());
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            assertEquals("admin:password123", decoded);
        }

        @Test
        @DisplayName("Should encode UTF-8 credentials correctly (German umlauts)")
        void shouldEncodeUtf8GermanCredentials() {
            AtomicReference<String> authHeaderRef = new AtomicReference<>();

            server.createContext("/control/status", exchange -> {
                List<String> authHeaders = exchange.getRequestHeaders().get("Authorization");
                if (authHeaders != null && !authHeaders.isEmpty()) {
                    authHeaderRef.set(authHeaders.get(0));
                }
                respondJson(exchange, 200, "{\"version\":\"0.107.0\"}");
            });

            // German umlauts: ü, ö, ä
            DnsBlockerConfig config = new DnsBlockerConfig(
                    DnsBlockerType.ADGUARD_HOME, "localhost", port, "http", "müller", "passwörd");
            AdGuardHomeHandler handler = new AdGuardHomeHandler(config, new HttpClientUtil());

            handler.getVersion();

            assertNotNull(authHeaderRef.get());
            String encoded = authHeaderRef.get().substring("Basic ".length());
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            assertEquals("müller:passwörd", decoded);
        }

        @Test
        @DisplayName("Should encode UTF-8 credentials correctly (Chinese characters)")
        void shouldEncodeUtf8ChineseCredentials() {
            AtomicReference<String> authHeaderRef = new AtomicReference<>();

            server.createContext("/control/status", exchange -> {
                List<String> authHeaders = exchange.getRequestHeaders().get("Authorization");
                if (authHeaders != null && !authHeaders.isEmpty()) {
                    authHeaderRef.set(authHeaders.get(0));
                }
                respondJson(exchange, 200, "{\"version\":\"0.107.0\"}");
            });

            // Chinese characters
            DnsBlockerConfig config = new DnsBlockerConfig(
                    DnsBlockerType.ADGUARD_HOME, "localhost", port, "http", "用户", "密码");
            AdGuardHomeHandler handler = new AdGuardHomeHandler(config, new HttpClientUtil());

            handler.getVersion();

            assertNotNull(authHeaderRef.get());
            String encoded = authHeaderRef.get().substring("Basic ".length());
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            assertEquals("用户:密码", decoded);
        }

        @Test
        @DisplayName("Should encode UTF-8 credentials correctly (Japanese characters)")
        void shouldEncodeUtf8JapaneseCredentials() {
            AtomicReference<String> authHeaderRef = new AtomicReference<>();

            server.createContext("/control/status", exchange -> {
                List<String> authHeaders = exchange.getRequestHeaders().get("Authorization");
                if (authHeaders != null && !authHeaders.isEmpty()) {
                    authHeaderRef.set(authHeaders.get(0));
                }
                respondJson(exchange, 200, "{\"version\":\"0.107.0\"}");
            });

            // Japanese hiragana and katakana
            DnsBlockerConfig config = new DnsBlockerConfig(
                    DnsBlockerType.ADGUARD_HOME, "localhost", port, "http", "ユーザー", "パスワード");
            AdGuardHomeHandler handler = new AdGuardHomeHandler(config, new HttpClientUtil());

            handler.getVersion();

            assertNotNull(authHeaderRef.get());
            String encoded = authHeaderRef.get().substring("Basic ".length());
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            assertEquals("ユーザー:パスワード", decoded);
        }

        @Test
        @DisplayName("Should encode UTF-8 credentials correctly (Russian Cyrillic)")
        void shouldEncodeUtf8RussianCredentials() {
            AtomicReference<String> authHeaderRef = new AtomicReference<>();

            server.createContext("/control/status", exchange -> {
                List<String> authHeaders = exchange.getRequestHeaders().get("Authorization");
                if (authHeaders != null && !authHeaders.isEmpty()) {
                    authHeaderRef.set(authHeaders.get(0));
                }
                respondJson(exchange, 200, "{\"version\":\"0.107.0\"}");
            });

            // Russian Cyrillic
            DnsBlockerConfig config = new DnsBlockerConfig(
                    DnsBlockerType.ADGUARD_HOME, "localhost", port, "http", "пользователь", "пароль");
            AdGuardHomeHandler handler = new AdGuardHomeHandler(config, new HttpClientUtil());

            handler.getVersion();

            assertNotNull(authHeaderRef.get());
            String encoded = authHeaderRef.get().substring("Basic ".length());
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            assertEquals("пользователь:пароль", decoded);
        }

        @Test
        @DisplayName("Should encode UTF-8 credentials with special characters")
        void shouldEncodeSpecialCharacters() {
            AtomicReference<String> authHeaderRef = new AtomicReference<>();

            server.createContext("/control/status", exchange -> {
                List<String> authHeaders = exchange.getRequestHeaders().get("Authorization");
                if (authHeaders != null && !authHeaders.isEmpty()) {
                    authHeaderRef.set(authHeaders.get(0));
                }
                respondJson(exchange, 200, "{\"version\":\"0.107.0\"}");
            });

            // Mixed special characters: emoji, accented letters, symbols
            String username = "admin@home";
            String password = "P@$$w0rd!#€£¥";
            DnsBlockerConfig config = new DnsBlockerConfig(
                    DnsBlockerType.ADGUARD_HOME, "localhost", port, "http", username, password);
            AdGuardHomeHandler handler = new AdGuardHomeHandler(config, new HttpClientUtil());

            handler.getVersion();

            assertNotNull(authHeaderRef.get());
            String encoded = authHeaderRef.get().substring("Basic ".length());
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            assertEquals(username + ":" + password, decoded);
        }

        @Test
        @DisplayName("Should not send auth header when credentials are blank")
        void shouldNotSendAuthHeaderWhenCredentialsBlank() {
            AtomicReference<List<String>> authHeaderRef = new AtomicReference<>();

            server.createContext("/control/status", exchange -> {
                authHeaderRef.set(exchange.getRequestHeaders().get("Authorization"));
                respondJson(exchange, 200, "{\"version\":\"0.107.0\"}");
            });

            DnsBlockerConfig config = new DnsBlockerConfig(
                    DnsBlockerType.ADGUARD_HOME, "localhost", port, "http", "", "");
            AdGuardHomeHandler handler = new AdGuardHomeHandler(config, new HttpClientUtil());

            handler.getVersion();

            assertNull(authHeaderRef.get(), "Should not send Authorization header with blank credentials");
        }
    }

    // ==================== API Response Tests ====================

    @Nested
    @DisplayName("API Response Handling")
    class ApiResponseTests {

        @Test
        @DisplayName("getStats should transform to generic schema")
        void getStatsShouldTransformToGenericSchema() {
            server.createContext("/control/stats", exchange -> {
                respondJson(exchange, 200,
                        "{\"num_dns_queries\":1000,\"num_blocked_filtering\":250,\"avg_processing_time\":0.5}");
            });

            server.createContext("/control/filtering/status", exchange -> {
                respondJson(exchange, 200,
                        "{\"filters\":[{\"enabled\":true,\"rules_count\":50000}]}");
            });

            DnsBlockerConfig config = new DnsBlockerConfig(
                    DnsBlockerType.ADGUARD_HOME, "localhost", port, "http", "admin", "pass");
            AdGuardHomeHandler handler = new AdGuardHomeHandler(config, new HttpClientUtil());

            String result = handler.getStats();

            assertNotNull(result);
            assertTrue(result.contains("\"schema\":\"dnsblocker.stats.v1\""));
            assertTrue(result.contains("\"source\":\"adguard-home\""));
            assertTrue(result.contains("\"total\":1000"));
            assertTrue(result.contains("\"blocked\":250"));
            // 250/1000 = 25%
            assertTrue(result.contains("25.0") || result.contains("\"percent_blocked\":25"));
        }

        @Test
        @DisplayName("getVersion should extract version string")
        void getVersionShouldExtractVersionString() {
            server.createContext("/control/status", exchange -> {
                respondJson(exchange, 200, "{\"version\":\"0.107.43\",\"protection_enabled\":true}");
            });

            DnsBlockerConfig config = new DnsBlockerConfig(
                    DnsBlockerType.ADGUARD_HOME, "localhost", port, "http", "admin", "pass");
            AdGuardHomeHandler handler = new AdGuardHomeHandler(config, new HttpClientUtil());

            String version = handler.getVersion();

            assertEquals("0.107.43", version);
        }

        @Test
        @DisplayName("getLastBlocked should extract domain from query log")
        void getLastBlockedShouldExtractDomain() {
            server.createContext("/control/querylog", exchange -> {
                respondJson(exchange, 200,
                        "{\"data\":[{\"question\":{\"name\":\"ads.example.com\",\"type\":\"A\"}}]}");
            });

            DnsBlockerConfig config = new DnsBlockerConfig(
                    DnsBlockerType.ADGUARD_HOME, "localhost", port, "http", "admin", "pass");
            AdGuardHomeHandler handler = new AdGuardHomeHandler(config, new HttpClientUtil());

            String lastBlocked = handler.getLastBlocked();

            assertEquals("ads.example.com", lastBlocked);
        }

        @Test
        @DisplayName("getTopXBlocked should transform to generic schema")
        void getTopXBlockedShouldTransformToGenericSchema() {
            server.createContext("/control/stats", exchange -> {
                respondJson(exchange, 200,
                        "{\"top_blocked_domains\":[{\"doubleclick.net\":150},{\"ads.google.com\":100}]}");
            });

            DnsBlockerConfig config = new DnsBlockerConfig(
                    DnsBlockerType.ADGUARD_HOME, "localhost", port, "http", "admin", "pass");
            AdGuardHomeHandler handler = new AdGuardHomeHandler(config, new HttpClientUtil());

            String result = handler.getTopXBlocked(5);

            assertNotNull(result);
            assertTrue(result.contains("\"schema\":\"dnsblocker.top_blocked.v1\""));
            assertTrue(result.contains("\"doubleclick.net\""));
            assertTrue(result.contains("150"));
            assertTrue(result.contains("\"ads.google.com\""));
            assertTrue(result.contains("100"));
        }

        @Test
        @DisplayName("getDnsBlockingStatus should transform to generic schema")
        void getDnsBlockingStatusShouldTransformToGenericSchema() {
            server.createContext("/control/status", exchange -> {
                respondJson(exchange, 200, "{\"version\":\"0.107.0\",\"protection_enabled\":true}");
            });

            DnsBlockerConfig config = new DnsBlockerConfig(
                    DnsBlockerType.ADGUARD_HOME, "localhost", port, "http", "admin", "pass");
            AdGuardHomeHandler handler = new AdGuardHomeHandler(config, new HttpClientUtil());

            String result = handler.getDnsBlockingStatus();

            assertNotNull(result);
            assertTrue(result.contains("\"schema\":\"dnsblocker.blocking_status.v1\""));
            assertTrue(result.contains("\"enabled\":true"));
        }

        @Test
        @DisplayName("getDnsBlockingStatus should handle disabled protection")
        void getDnsBlockingStatusShouldHandleDisabled() {
            server.createContext("/control/status", exchange -> {
                respondJson(exchange, 200, "{\"version\":\"0.107.0\",\"protection_enabled\":false}");
            });

            DnsBlockerConfig config = new DnsBlockerConfig(
                    DnsBlockerType.ADGUARD_HOME, "localhost", port, "http", "admin", "pass");
            AdGuardHomeHandler handler = new AdGuardHomeHandler(config, new HttpClientUtil());

            String result = handler.getDnsBlockingStatus();

            assertNotNull(result);
            assertTrue(result.contains("\"enabled\":false"));
        }
    }

    // ==================== Authentication Tests ====================

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("authenticate should return true on successful status call")
        void authenticateShouldReturnTrueOnSuccess() {
            server.createContext("/control/status", exchange -> {
                respondJson(exchange, 200, "{\"version\":\"0.107.0\"}");
            });

            DnsBlockerConfig config = new DnsBlockerConfig(
                    DnsBlockerType.ADGUARD_HOME, "localhost", port, "http", "admin", "pass");
            AdGuardHomeHandler handler = new AdGuardHomeHandler(config, new HttpClientUtil());

            boolean result = handler.authenticate();

            assertTrue(result);
        }

        @Test
        @DisplayName("authenticate should return false on 401 response")
        void authenticateShouldReturnFalseOn401() {
            server.createContext("/control/status", exchange -> {
                respondJson(exchange, 401, "{\"error\":\"unauthorized\"}");
            });

            DnsBlockerConfig config = new DnsBlockerConfig(
                    DnsBlockerType.ADGUARD_HOME, "localhost", port, "http", "admin", "wrongpass");
            AdGuardHomeHandler handler = new AdGuardHomeHandler(config, new HttpClientUtil());

            boolean result = handler.authenticate();

            assertFalse(result);
        }

        @Test
        @DisplayName("authenticate should return false when credentials are blank")
        void authenticateShouldReturnFalseWhenCredentialsBlank() {
            DnsBlockerConfig config = new DnsBlockerConfig(
                    DnsBlockerType.ADGUARD_HOME, "localhost", port, "http", "", "");
            AdGuardHomeHandler handler = new AdGuardHomeHandler(config, new HttpClientUtil());

            boolean result = handler.authenticate();

            assertFalse(result);
        }
    }

    // ==================== Error Handling Tests ====================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("getStats should return empty string on HTTP error")
        void getStatsShouldReturnEmptyOnError() {
            server.createContext("/control/stats", exchange -> {
                respondJson(exchange, 500, "{\"error\":\"internal server error\"}");
            });

            DnsBlockerConfig config = new DnsBlockerConfig(
                    DnsBlockerType.ADGUARD_HOME, "localhost", port, "http", "admin", "pass");
            AdGuardHomeHandler handler = new AdGuardHomeHandler(config, new HttpClientUtil());

            String result = handler.getStats();

            assertEquals("", result);
        }

        @Test
        @DisplayName("getTopXBlocked should return empty string for count <= 0")
        void getTopXBlockedShouldReturnEmptyForInvalidCount() {
            DnsBlockerConfig config = new DnsBlockerConfig(
                    DnsBlockerType.ADGUARD_HOME, "localhost", port, "http", "admin", "pass");
            AdGuardHomeHandler handler = new AdGuardHomeHandler(config, new HttpClientUtil());

            assertEquals("", handler.getTopXBlocked(0));
            assertEquals("", handler.getTopXBlocked(-1));
        }
    }

    // ==================== Helper Methods ====================

    private void respondJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        } finally {
            exchange.close();
        }
    }

}

