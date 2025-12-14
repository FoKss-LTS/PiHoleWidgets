package domain.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PiholeConfig record.
 */
class PiholeConfigTest {

    @Test
    void testDefaultConstructor() {
        var config = new PiholeConfig("192.168.1.1", 80, "http", "token123");
        
        assertEquals("192.168.1.1", config.ipAddress());
        assertEquals(80, config.port());
        assertEquals("http", config.scheme());
        assertEquals("token123", config.authToken());
    }

    @Test
    void testConstructorWithDefaultPort() {
        var config = new PiholeConfig("192.168.1.1", "https", "token123");
        
        assertEquals("192.168.1.1", config.ipAddress());
        assertEquals(PiholeConfig.DEFAULT_PORT, config.port());
        assertEquals("https", config.scheme());
        assertEquals("token123", config.authToken());
    }

    @Test
    void testNullSchemeDefaultsToHttp() {
        var config = new PiholeConfig("192.168.1.1", 80, null, "token");
        
        assertEquals(PiholeConfig.DEFAULT_SCHEME, config.scheme());
    }

    @Test
    void testBlankSchemeDefaultsToHttp() {
        var config = new PiholeConfig("192.168.1.1", 80, "   ", "token");
        
        assertEquals(PiholeConfig.DEFAULT_SCHEME, config.scheme());
    }

    @Test
    void testNullIpAddressDefaultsToEmpty() {
        var config = new PiholeConfig(null, 80, "http", "token");
        
        assertEquals("", config.ipAddress());
    }

    @Test
    void testNullAuthTokenDefaultsToEmpty() {
        var config = new PiholeConfig("192.168.1.1", 80, "http", null);
        
        assertEquals(PiholeConfig.DEFAULT_AUTH_TOKEN, config.authToken());
    }

    @Test
    void testInvalidPortDefaultsToDefaultPort() {
        var config1 = new PiholeConfig("192.168.1.1", 0, "http", "token");
        assertEquals(PiholeConfig.DEFAULT_PORT, config1.port());
        
        var config2 = new PiholeConfig("192.168.1.1", -1, "http", "token");
        assertEquals(PiholeConfig.DEFAULT_PORT, config2.port());
        
        var config3 = new PiholeConfig("192.168.1.1", 65536, "http", "token");
        assertEquals(PiholeConfig.DEFAULT_PORT, config3.port());
    }

    @Test
    void testValidPortRange() {
        var config1 = new PiholeConfig("192.168.1.1", 1, "http", "token");
        assertEquals(1, config1.port());
        
        var config2 = new PiholeConfig("192.168.1.1", 65535, "http", "token");
        assertEquals(65535, config2.port());
        
        var config3 = new PiholeConfig("192.168.1.1", 443, "https", "token");
        assertEquals(443, config3.port());
    }

    @Test
    void testLegacyGetters() {
        var config = new PiholeConfig("192.168.1.1", 80, "http", "token123");
        
        assertEquals("192.168.1.1", config.getIPAddress());
        assertEquals(80, config.getPort());
        assertEquals("http", config.getScheme());
        assertEquals("token123", config.getAUTH());
    }

    @Test
    void testHasValidAddress() {
        var validConfig = new PiholeConfig("192.168.1.1", 80, "http", "token");
        assertTrue(validConfig.hasValidAddress());
        
        var invalidConfig1 = new PiholeConfig("", 80, "http", "token");
        assertFalse(invalidConfig1.hasValidAddress());
        
        var invalidConfig2 = new PiholeConfig(null, 80, "http", "token");
        assertFalse(invalidConfig2.hasValidAddress());
        
        var invalidConfig3 = new PiholeConfig("   ", 80, "http", "token");
        assertFalse(invalidConfig3.hasValidAddress());
    }

    @Test
    void testBuildBaseUrl() {
        var config = new PiholeConfig("192.168.1.1", 80, "http", "token");
        assertEquals("http://192.168.1.1:80", config.buildBaseUrl());
        
        var config2 = new PiholeConfig("pi.hole", 443, "https", "token");
        assertEquals("https://pi.hole:443", config2.buildBaseUrl());
    }

    @Test
    void testRecordEquality() {
        var config1 = new PiholeConfig("192.168.1.1", 80, "http", "token");
        var config2 = new PiholeConfig("192.168.1.1", 80, "http", "token");
        var config3 = new PiholeConfig("192.168.1.2", 80, "http", "token");
        
        assertEquals(config1, config2);
        assertNotEquals(config1, config3);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    void testToString() {
        var config = new PiholeConfig("192.168.1.1", 80, "http", "token");
        var toString = config.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("192.168.1.1"));
        assertTrue(toString.contains("80"));
    }
}

