package helpers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HelperService utility class.
 */
class HelperServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void testCreateFileCreatesDirectoryIfNotExists() {
        String home = tempDir.toString();
        String folderName = "TestFolder";
        String fileName = "test.txt";
        
        File result = HelperService.createFile(home, fileName, folderName);
        
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.isFile());
        
        Path folderPath = tempDir.resolve(folderName);
        assertTrue(Files.exists(folderPath));
    }

    @Test
    void testCreateFileReturnsExistingFile() {
        String home = tempDir.toString();
        String folderName = "TestFolder";
        String fileName = "test.txt";
        
        // Create file first time
        File first = HelperService.createFile(home, fileName, folderName);
        assertNotNull(first);
        
        // Create file second time (should return existing)
        File second = HelperService.createFile(home, fileName, folderName);
        assertNotNull(second);
        assertEquals(first.getAbsolutePath(), second.getAbsolutePath());
    }

    @Test
    void testCreateFolderCreatesDirectory() {
        String folderPath = tempDir.resolve("NewFolder").toString();
        
        boolean result = HelperService.createFolder(folderPath);
        
        assertTrue(result);
        assertTrue(Files.exists(Path.of(folderPath)));
    }

    @Test
    void testCreateFolderReturnsTrueIfExists() {
        String folderPath = tempDir.toString();
        
        // Directory already exists (tempDir)
        boolean result = HelperService.createFolder(folderPath);
        
        assertTrue(result);
    }

    @Test
    void testGetHumanReadablePriceFromNumber() {
        assertEquals("0", HelperService.getHumanReadablePriceFromNumber(0));
        assertEquals("1", HelperService.getHumanReadablePriceFromNumber(1));
        assertEquals("1,234", HelperService.getHumanReadablePriceFromNumber(1234));
        assertEquals("1,234,567", HelperService.getHumanReadablePriceFromNumber(1234567));
        assertEquals("999,999,999", HelperService.getHumanReadablePriceFromNumber(999999999));
    }

    @Test
    void testGetHumanReadablePriceFromNumberWithLargeNumbers() {
        long largeNumber = 1234567890123L;
        String result = HelperService.getHumanReadablePriceFromNumber(largeNumber);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // Format depends on locale, but should contain digits
        assertTrue(result.matches(".*\\d.*"));
    }

    @Test
    void testParseLongSafeWithValidNumber() {
        assertEquals(123L, HelperService.parseLongSafe("123", 0));
        assertEquals(456L, HelperService.parseLongSafe(456, 0));
        assertEquals(789L, HelperService.parseLongSafe(789L, 0));
    }

    @Test
    void testParseLongSafeWithFormattedNumber() {
        assertEquals(1234L, HelperService.parseLongSafe("1,234", 0));
        assertEquals(1234567L, HelperService.parseLongSafe("1,234,567", 0));
    }

    @Test
    void testParseLongSafeWithNull() {
        assertEquals(999L, HelperService.parseLongSafe(null, 999));
    }

    @Test
    void testParseLongSafeWithInvalidString() {
        assertEquals(999L, HelperService.parseLongSafe("invalid", 999));
        assertEquals(999L, HelperService.parseLongSafe("abc123", 999));
    }

    @Test
    void testParseLongSafeWithEmptyString() {
        assertEquals(999L, HelperService.parseLongSafe("", 999));
    }

    @Test
    void testParseDoubleSafeWithValidNumber() {
        assertEquals(123.45, HelperService.parseDoubleSafe("123.45", 0.0), 0.001);
        assertEquals(456.78, HelperService.parseDoubleSafe(456.78, 0.0), 0.001);
        assertEquals(789.0, HelperService.parseDoubleSafe(789.0, 0.0), 0.001);
    }

    @Test
    void testParseDoubleSafeWithFormattedNumber() {
        assertEquals(1234.56, HelperService.parseDoubleSafe("1,234.56", 0.0), 0.001);
        assertEquals(1234567.89, HelperService.parseDoubleSafe("1,234,567.89", 0.0), 0.001);
    }

    @Test
    void testParseDoubleSafeWithNull() {
        assertEquals(999.99, HelperService.parseDoubleSafe(null, 999.99), 0.001);
    }

    @Test
    void testParseDoubleSafeWithInvalidString() {
        assertEquals(999.99, HelperService.parseDoubleSafe("invalid", 999.99), 0.001);
        assertEquals(999.99, HelperService.parseDoubleSafe("abc123.45", 999.99), 0.001);
    }

    @Test
    void testParseDoubleSafeWithEmptyString() {
        assertEquals(999.99, HelperService.parseDoubleSafe("", 999.99), 0.001);
    }

    @Test
    void testConvertJsonToLongDeprecated() {
        assertEquals("123", HelperService.convertJsonToLong("123"));
        assertEquals("1234", HelperService.convertJsonToLong("1,234"));
        assertEquals("0", HelperService.convertJsonToLong(null));
        assertEquals("", HelperService.convertJsonToLong(""));
    }
}

