package ai.luumo.tools.picodingagent.reporting.util;

import java.time.Duration;
import java.time.LocalDateTime;

public class TimeFormatter {
    
    public static String formatRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "unknown";
        }
        
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(dateTime, now);
        long seconds = Math.abs(duration.getSeconds());
        boolean isPast = duration.getSeconds() >= 0;
        
        if (seconds < 5) {
            return "just now";
        }
        
        if (seconds < 60) {
            return formatTimeUnit(seconds, "second", isPast);
        }
        
        long minutes = seconds / 60;
        if (minutes < 60) {
            return formatTimeUnit(minutes, "minute", isPast);
        }
        
        long hours = minutes / 60;
        if (hours < 24) {
            return formatTimeUnit(hours, "hour", isPast);
        }
        
        long days = hours / 24;
        if (days < 7) {
            return formatTimeUnit(days, "day", isPast);
        }
        
        long weeks = days / 7;
        if (weeks < 4) {
            return formatTimeUnit(weeks, "week", isPast);
        }
        
        // For older dates, return formatted date
        return dateTime.toLocalDate().toString();
    }
    
    private static String formatTimeUnit(long amount, String unit, boolean isPast) {
        String plural = amount == 1 ? "" : "s";
        if (isPast) {
            return amount + " " + unit + plural + " ago";
        } else {
            return "in " + amount + " " + unit + plural;
        }
    }
}
