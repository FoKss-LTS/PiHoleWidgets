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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class providing file operations and formatting helpers.
 */
public final class HelperService {

    private static final Logger LOGGER = Logger.getLogger(HelperService.class.getName());
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("pihole.verbose", "false"));
    
    // Number formatter for human-readable numbers (thread-safe via ThreadLocal)
    private static final ThreadLocal<NumberFormat> NUMBER_FORMAT = 
            ThreadLocal.withInitial(() -> NumberFormat.getIntegerInstance(Locale.getDefault()));
    
    // Private constructor to prevent instantiation
    private HelperService() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    private static void log(String message) {
        if (VERBOSE) {
            LOGGER.log(Level.FINE, () -> "[Helper] " + message);
        }
    }

    /**
     * Creates a file in the specified folder under the home directory.
     * Creates the parent folder if it doesn't exist.
     *
     * @param home the base directory (typically user.home)
     * @param fileName the name of the file to create
     * @param folderName the name of the folder to create the file in
     * @return the created File, or null if creation failed
     */
    public static java.io.File createFile(String home, String fileName, String folderName) {
        Path folderPath = Path.of(home, folderName);
        Path filePath = folderPath.resolve(fileName);
        
        log("Creating file: " + filePath);
        
        try {
            // Create folder if it doesn't exist
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
                log("Created directory: " + folderPath);
            }
            
            // Create file if it doesn't exist
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
                log("Created file: " + filePath);
            } else {
                log("File already exists: " + filePath);
            }
            
            return filePath.toFile();
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create file: " + filePath, e);
            return null;
        }
    }

    /**
     * Creates a folder at the specified path if it doesn't exist.
     *
     * @param folderPath the path of the folder to create
     * @return true if the folder exists or was created successfully, false otherwise
     */
    public static boolean createFolder(String folderPath) {
        Path path = Path.of(folderPath);
        
        if (Files.exists(path)) {
            return true;
        }
        
        try {
            Files.createDirectories(path);
            log("Created folder: " + folderPath);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create folder: " + folderPath, e);
            return false;
        }
    }

    /**
     * Formats a number with locale-appropriate grouping separators.
     * Example: 1234567 -> "1,234,567" (for US locale)
     *
     * @param number the number to format
     * @return the formatted string
     */
    public static String getHumanReadablePriceFromNumber(long number) {
        return NUMBER_FORMAT.get().format(number);
    }

    /**
     * Removes commas from a JSON number string for parsing.
     * Used for backward compatibility with formatted numbers in JSON.
     *
     * @param obj the object to convert (expected to be a String)
     * @return the string with commas removed
     * @deprecated Use proper JSON parsing instead
     */
    @Deprecated(forRemoval = true)
    public static String convertJsonToLong(Object obj) {
        if (obj == null) {
            return "0";
        }
        return obj.toString().replace(",", "");
    }
    
    /**
     * Safely parses a long from an object.
     *
     * @param obj the object to parse
     * @param defaultValue the default value if parsing fails
     * @return the parsed long or defaultValue
     */
    public static long parseLongSafe(Object obj, long defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        
        try {
            String str = obj.toString().replace(",", "");
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Safely parses a double from an object.
     *
     * @param obj the object to parse
     * @param defaultValue the default value if parsing fails
     * @return the parsed double or defaultValue
     */
    public static double parseDoubleSafe(Object obj, double defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        
        try {
            String str = obj.toString().replace(",", "");
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
