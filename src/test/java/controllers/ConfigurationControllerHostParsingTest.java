package controllers;

import domain.configuration.DnsBlockerConfig;
import domain.configuration.WidgetConfig;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ConfigurationController's host input parsing logic.
 * 
 * The parseHostInput method accepts various user input formats:
 * - Simple hostname: pi.hole
 * - IP addresses: 192.168.1.1
 * - Host with port: pi.hole:8080
 * - Full URLs: http://pi.hole:8080, https://192.168.1.1:443
 * - IPv6 addresses: [::1], [::1]:8080
 * 
 * It must reject inputs with paths, query strings, or fragments.
 */
class ConfigurationControllerHostParsingTest {

    private ConfigurationController controller;
    private Method parseHostInputMethod;
    private Method parseHostPortMethod;
    private static final AppActions NOOP_ACTIONS = new AppActions() {
        @Override public void openConfigurationWindow() {}
        @Override public void applyAndCloseConfigurationWindow() {}
        @Override public void closeConfigurationWindow() {}
        @Override public void hideToTray() {}
        @Override public void requestExit() {}
    };

    @BeforeAll
    static void initJavaFX() {
        new JFXPanel();
    }

    @BeforeEach
    void setUp() throws Exception {
        DnsBlockerConfig testConfig = DnsBlockerConfig.forPiHole("192.168.1.1", 80, "http", "testtoken");
        WidgetConfig testWidgetConfig = new WidgetConfig("Medium", "Square", "Dark");
        controller = new ConfigurationController(testConfig, null, testWidgetConfig, NOOP_ACTIONS);

        // Get access to private parseHostInput method via reflection
        parseHostInputMethod = ConfigurationController.class.getDeclaredMethod("parseHostInput", String.class);
        parseHostInputMethod.setAccessible(true);

        parseHostPortMethod = ConfigurationController.class.getDeclaredMethod("parseHostPort", String.class);
        parseHostPortMethod.setAccessible(true);
    }

    /**
     * Invokes the private parseHostInput method and returns the result.
     */
    private ParsedHostInputWrapper parseHostInput(String raw) throws Exception {
        Object result = parseHostInputMethod.invoke(controller, raw);
        return new ParsedHostInputWrapper(result);
    }

    /**
     * Invokes the private parseHostPort method and returns the result.
     */
    private ParsedHostInputWrapper parseHostPort(String authority) throws Exception {
        Object result = parseHostPortMethod.invoke(controller, authority);
        return new ParsedHostInputWrapper(result);
    }

    /**
     * Wrapper to access ParsedHostInput record fields via reflection.
     */
    static class ParsedHostInputWrapper {
        private final Object record;

        ParsedHostInputWrapper(Object record) {
            this.record = record;
        }

        boolean valid() throws Exception {
            return (boolean) getRecordField("valid");
        }

        String host() throws Exception {
            return (String) getRecordField("host");
        }

        Integer port() throws Exception {
            return (Integer) getRecordField("port");
        }

        String scheme() throws Exception {
            return (String) getRecordField("scheme");
        }

        String errorMessage() throws Exception {
            return (String) getRecordField("errorMessage");
        }

        private Object getRecordField(String name) throws Exception {
            for (RecordComponent comp : record.getClass().getRecordComponents()) {
                if (comp.getName().equals(name)) {
                    return comp.getAccessor().invoke(record);
                }
            }
            throw new IllegalArgumentException("No field: " + name);
        }
    }

    // ==================== Empty/Null Input Tests ====================

    @Nested
    @DisplayName("Empty and Null Input Handling")
    class EmptyAndNullInputTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should accept null, empty, and whitespace inputs")
        void shouldAcceptNullEmptyAndWhitespaceInputs(String input) throws Exception {
            ParsedHostInputWrapper result = parseHostInput(input);
            assertTrue(result.valid(), "Should be valid for: '" + input + "'");
            assertEquals("", result.host(), "Host should be empty");
            assertNull(result.port(), "Port should be null");
            assertNull(result.scheme(), "Scheme should be null");
        }
    }

    // ==================== Simple Hostname Tests ====================

    @Nested
    @DisplayName("Simple Hostname Parsing")
    class SimpleHostnameTests {

        @ParameterizedTest
        @ValueSource(strings = {"pi.hole", "pihole", "localhost", "my-server", "server123"})
        @DisplayName("Should parse simple hostnames")
        void shouldParseSimpleHostnames(String hostname) throws Exception {
            ParsedHostInputWrapper result = parseHostInput(hostname);
            assertTrue(result.valid());
            assertEquals(hostname, result.host());
            assertNull(result.port());
            assertNull(result.scheme());
        }

        @Test
        @DisplayName("Should handle hostname with trailing spaces")
        void shouldHandleHostnameWithTrailingSpaces() throws Exception {
            ParsedHostInputWrapper result = parseHostInput("  pi.hole  ");
            assertTrue(result.valid());
            assertEquals("pi.hole", result.host());
        }
    }

    // ==================== IP Address Tests ====================

    @Nested
    @DisplayName("IP Address Parsing")
    class IpAddressTests {

        @ParameterizedTest
        @ValueSource(strings = {"192.168.1.1", "10.0.0.1", "127.0.0.1", "255.255.255.255"})
        @DisplayName("Should parse IPv4 addresses")
        void shouldParseIPv4Addresses(String ip) throws Exception {
            ParsedHostInputWrapper result = parseHostInput(ip);
            assertTrue(result.valid());
            assertEquals(ip, result.host());
            assertNull(result.port());
            assertNull(result.scheme());
        }

        @ParameterizedTest
        @ValueSource(strings = {"::1", "fe80::1", "2001:db8::1", "2001:0db8:85a3:0000:0000:8a2e:0370:7334"})
        @DisplayName("Should parse unbracketed IPv6 addresses (no port)")
        void shouldParseUnbracketedIPv6Addresses(String ip) throws Exception {
            ParsedHostInputWrapper result = parseHostInput(ip);
            assertTrue(result.valid());
            // Multiple colons mean it's treated as IPv6 without port
            assertNotNull(result.host());
        }
    }

    // ==================== Host:Port Tests ====================

    @Nested
    @DisplayName("Host:Port Parsing")
    class HostPortTests {

        @ParameterizedTest
        @CsvSource({
            "pi.hole:8080, pi.hole, 8080",
            "localhost:80, localhost, 80",
            "192.168.1.1:443, 192.168.1.1, 443",
            "my-server:9999, my-server, 9999"
        })
        @DisplayName("Should parse host:port combinations")
        void shouldParseHostPortCombinations(String input, String expectedHost, int expectedPort) throws Exception {
            ParsedHostInputWrapper result = parseHostInput(input);
            assertTrue(result.valid(), "Should be valid for: " + input);
            assertEquals(expectedHost, result.host());
            assertEquals(expectedPort, result.port());
            assertNull(result.scheme());
        }

        @ParameterizedTest
        @CsvSource({
            "[::1]:8080, ::1, 8080",
            "[fe80::1]:443, fe80::1, 443",
            "[2001:db8::1]:80, 2001:db8::1, 80"
        })
        @DisplayName("Should parse bracketed IPv6 with port")
        void shouldParseBracketedIPv6WithPort(String input, String expectedHost, int expectedPort) throws Exception {
            ParsedHostInputWrapper result = parseHostInput(input);
            assertTrue(result.valid(), "Should be valid for: " + input);
            assertEquals(expectedHost, result.host());
            assertEquals(expectedPort, result.port());
        }

        @Test
        @DisplayName("Should parse bracketed IPv6 without port")
        void shouldParseBracketedIPv6WithoutPort() throws Exception {
            ParsedHostInputWrapper result = parseHostInput("[::1]");
            assertTrue(result.valid());
            assertEquals("::1", result.host());
            assertNull(result.port());
        }
    }

    // ==================== URL Parsing Tests ====================

    @Nested
    @DisplayName("Full URL Parsing")
    class UrlParsingTests {

        @ParameterizedTest
        @CsvSource({
            "http://pi.hole, pi.hole, , http",
            "https://pi.hole, pi.hole, , https",
            "http://192.168.1.1, 192.168.1.1, , http",
            "https://10.0.0.1, 10.0.0.1, , https"
        })
        @DisplayName("Should parse URLs without port")
        void shouldParseUrlsWithoutPort(String input, String expectedHost, Integer expectedPort, String expectedScheme) throws Exception {
            ParsedHostInputWrapper result = parseHostInput(input);
            assertTrue(result.valid(), "Should be valid for: " + input);
            assertEquals(expectedHost, result.host());
            assertEquals(expectedPort, result.port());
            assertEquals(expectedScheme, result.scheme());
        }

        @ParameterizedTest
        @CsvSource({
            "http://pi.hole:8080, pi.hole, 8080, http",
            "https://pi.hole:443, pi.hole, 443, https",
            "http://192.168.1.1:80, 192.168.1.1, 80, http",
            "https://10.0.0.1:9443, 10.0.0.1, 9443, https"
        })
        @DisplayName("Should parse URLs with port")
        void shouldParseUrlsWithPort(String input, String expectedHost, int expectedPort, String expectedScheme) throws Exception {
            ParsedHostInputWrapper result = parseHostInput(input);
            assertTrue(result.valid(), "Should be valid for: " + input);
            assertEquals(expectedHost, result.host());
            assertEquals(expectedPort, result.port());
            assertEquals(expectedScheme, result.scheme());
        }

        @Test
        @DisplayName("Should accept URL with trailing slash (root path)")
        void shouldAcceptUrlWithTrailingSlash() throws Exception {
            ParsedHostInputWrapper result = parseHostInput("http://pi.hole/");
            assertTrue(result.valid());
            assertEquals("pi.hole", result.host());
            assertEquals("http", result.scheme());
        }
    }

    // ==================== Invalid Input Tests ====================

    @Nested
    @DisplayName("Invalid Input Rejection")
    class InvalidInputTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "http://pi.hole/admin",
            "http://pi.hole/api/stats",
            "pi.hole/admin",
            "192.168.1.1/path"
        })
        @DisplayName("Should reject inputs with paths")
        void shouldRejectInputsWithPaths(String input) throws Exception {
            ParsedHostInputWrapper result = parseHostInput(input);
            assertFalse(result.valid(), "Should reject: " + input);
            assertTrue(result.errorMessage().contains("path"), 
                "Error should mention path: " + result.errorMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "http://pi.hole?query=value",
            "pi.hole?something=1"
        })
        @DisplayName("Should reject inputs with query strings")
        void shouldRejectInputsWithQueryStrings(String input) throws Exception {
            ParsedHostInputWrapper result = parseHostInput(input);
            assertFalse(result.valid(), "Should reject: " + input);
            assertTrue(result.errorMessage().contains("query") || result.errorMessage().contains("fragment"),
                "Error should mention query/fragment: " + result.errorMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "http://pi.hole#section",
            "pi.hole#anchor"
        })
        @DisplayName("Should reject inputs with fragments")
        void shouldRejectInputsWithFragments(String input) throws Exception {
            ParsedHostInputWrapper result = parseHostInput(input);
            assertFalse(result.valid(), "Should reject: " + input);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "pi.hole:abc",
            "pi.hole:99999",
            "pi.hole:-1",
            "pi.hole:0"
        })
        @DisplayName("Should reject invalid port numbers")
        void shouldRejectInvalidPortNumbers(String input) throws Exception {
            ParsedHostInputWrapper result = parseHostInput(input);
            assertFalse(result.valid(), "Should reject: " + input);
            assertTrue(result.errorMessage().toLowerCase().contains("port"),
                "Error should mention port: " + result.errorMessage());
        }

        @Test
        @DisplayName("Should reject port-only input (missing host)")
        void shouldRejectPortOnlyInput() throws Exception {
            ParsedHostInputWrapper result = parseHostInput(":8080");
            assertFalse(result.valid());
            assertTrue(result.errorMessage().toLowerCase().contains("host"),
                "Error should mention host is required: " + result.errorMessage());
        }

        @Test
        @DisplayName("Should reject empty bracketed IPv6")
        void shouldRejectEmptyBracketedIPv6() throws Exception {
            ParsedHostInputWrapper result = parseHostInput("[]:8080");
            assertFalse(result.valid());
            assertTrue(result.errorMessage().toLowerCase().contains("ipv6"),
                "Error should mention IPv6: " + result.errorMessage());
        }
    }

    // ==================== Edge Case Tests ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle port at boundary (1)")
        void shouldHandleMinPort() throws Exception {
            ParsedHostInputWrapper result = parseHostInput("pi.hole:1");
            assertTrue(result.valid());
            assertEquals(1, result.port());
        }

        @Test
        @DisplayName("Should handle port at boundary (65535)")
        void shouldHandleMaxPort() throws Exception {
            ParsedHostInputWrapper result = parseHostInput("pi.hole:65535");
            assertTrue(result.valid());
            assertEquals(65535, result.port());
        }

        @Test
        @DisplayName("Should reject port above 65535")
        void shouldRejectPortAbove65535() throws Exception {
            ParsedHostInputWrapper result = parseHostInput("pi.hole:65536");
            assertFalse(result.valid());
        }

        @Test
        @DisplayName("Should handle mixed case scheme")
        void shouldHandleMixedCaseScheme() throws Exception {
            ParsedHostInputWrapper result = parseHostInput("HTTP://pi.hole");
            assertTrue(result.valid());
            assertEquals("http", result.scheme(), "Scheme should be normalized to lowercase");
        }

        @Test
        @DisplayName("Should handle HTTPS mixed case")
        void shouldHandleHttpsMixedCase() throws Exception {
            ParsedHostInputWrapper result = parseHostInput("HTTPS://pi.hole:443");
            assertTrue(result.valid());
            assertEquals("https", result.scheme());
            assertEquals(443, result.port());
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "ftp://pi.hole",
            "ssh://pi.hole",
            "tcp://pi.hole"
        })
        @DisplayName("Should ignore non-HTTP schemes (return null scheme)")
        void shouldIgnoreNonHttpSchemes(String input) throws Exception {
            ParsedHostInputWrapper result = parseHostInput(input);
            // Non-http(s) schemes are not normalized
            assertTrue(result.valid());
            assertNull(result.scheme(), "Non-HTTP schemes should not be captured");
        }

        @Test
        @DisplayName("Should handle complex real-world URL")
        void shouldHandleComplexRealWorldUrl() throws Exception {
            ParsedHostInputWrapper result = parseHostInput("https://192.168.1.100:8443");
            assertTrue(result.valid());
            assertEquals("192.168.1.100", result.host());
            assertEquals(8443, result.port());
            assertEquals("https", result.scheme());
        }

        @Test
        @DisplayName("Should handle localhost URL")
        void shouldHandleLocalhostUrl() throws Exception {
            ParsedHostInputWrapper result = parseHostInput("http://localhost:3000");
            assertTrue(result.valid());
            assertEquals("localhost", result.host());
            assertEquals(3000, result.port());
            assertEquals("http", result.scheme());
        }
    }

    // ==================== parseHostPort Tests ====================

    @Nested
    @DisplayName("parseHostPort Method Tests")
    class ParseHostPortTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Should handle null, empty, and whitespace in parseHostPort")
        void shouldHandleEmptyInputsInParseHostPort(String input) throws Exception {
            ParsedHostInputWrapper result = parseHostPort(input);
            assertTrue(result.valid());
            assertEquals("", result.host());
        }

        @Test
        @DisplayName("Should parse simple host in parseHostPort")
        void shouldParseSimpleHostInParseHostPort() throws Exception {
            ParsedHostInputWrapper result = parseHostPort("pi.hole");
            assertTrue(result.valid());
            assertEquals("pi.hole", result.host());
            assertNull(result.port());
        }

        @Test
        @DisplayName("Should parse host:port in parseHostPort")
        void shouldParseHostPortInParseHostPort() throws Exception {
            ParsedHostInputWrapper result = parseHostPort("pi.hole:8080");
            assertTrue(result.valid());
            assertEquals("pi.hole", result.host());
            assertEquals(8080, result.port());
        }

        @Test
        @DisplayName("Should handle malformed bracketed IPv6")
        void shouldHandleMalformedBracketedIPv6() throws Exception {
            ParsedHostInputWrapper result = parseHostPort("[::1");
            // This is malformed - missing closing bracket
            // The implementation should handle this gracefully
            assertNotNull(result);
        }
    }
}

