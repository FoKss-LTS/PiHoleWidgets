/*
 *
 *  Copyright (C) 2022.  Reda ELFARISSI aka foxy999
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

package domain.pihole;

import java.util.StringJoiner;

/**
 * Represents Pi-hole gravity database update information.
 * Mutable for Jackson deserialization compatibility.
 */
public class Gravity {
    
    private boolean fileExists;
    private long absolute;
    private long days;
    private long hours;
    private long minutes;
    
    /**
     * Default constructor for Jackson deserialization.
     */
    public Gravity() {
    }
    
    /**
     * Full constructor.
     */
    public Gravity(boolean fileExists, long absolute, long days, long hours, long minutes) {
        this.fileExists = fileExists;
        this.absolute = absolute;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
    }
    
    // Getters
    
    public boolean isFileExists() {
        return fileExists;
    }
    
    // Alias for legacy compatibility
    public boolean isFile_exists() {
        return fileExists;
    }
    
    public long getAbsolute() {
        return absolute;
    }
    
    public long getDays() {
        return days;
    }
    
    public long getHours() {
        return hours;
    }
    
    public long getMinutes() {
        return minutes;
    }
    
    // Setters for Jackson deserialization
    
    public void setFileExists(boolean fileExists) {
        this.fileExists = fileExists;
    }
    
    // Alias for legacy compatibility
    public void setFile_exists(boolean fileExists) {
        this.fileExists = fileExists;
    }
    
    public void setAbsolute(long absolute) {
        this.absolute = absolute;
    }
    
    public void setDays(long days) {
        this.days = days;
    }
    
    public void setHours(long hours) {
        this.hours = hours;
    }
    
    public void setMinutes(long minutes) {
        this.minutes = minutes;
    }
    
    /**
     * Formats the gravity age as a human-readable string.
     * Example: "2 days 5 hours 30 mins"
     */
    public String formatAge() {
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append(days == 1 ? " day" : " days");
        }
        if (hours > 0) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        if (minutes > 0 || sb.isEmpty()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(minutes).append(minutes == 1 ? " min" : " mins");
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return new StringJoiner(", ", "Gravity[", "]")
                .add("fileExists=" + fileExists)
                .add("absolute=" + absolute)
                .add("days=" + days)
                .add("hours=" + hours)
                .add("minutes=" + minutes)
                .toString();
    }
}
