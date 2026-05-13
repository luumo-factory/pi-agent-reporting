package ai.luumo.tools.picodingagent.reporting.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Represents a report file with its metadata.
 * The filename field contains the relative path from the reports root directory
 * (e.g., "pi-agent-reporting/2026-05-13_test.md" or "2026-05-13_test.md" for root-level reports).
 */
public record Report(
    String filename,  // Relative path from reports root (e.g., "project/report.md")
    String title,
    String date,
    LocalDateTime lastModified,
    String project
) {
    
    /**
     * Returns the timestamp in ISO-8601 format with UTC timezone indicator.
     * This ensures JavaScript can parse it correctly regardless of browser timezone.
     * Client-side JavaScript computes relative times from this timestamp.
     */
    public String getTimestampISO() {
        return lastModified
            .atZone(ZoneId.systemDefault())
            .withZoneSameInstant(ZoneId.of("UTC"))
            .format(DateTimeFormatter.ISO_INSTANT);
    }
    
    /**
     * Returns just the filename portion without the path.
     * For example, "project/2026-05-13_test.md" returns "2026-05-13_test.md".
     * This is useful for display purposes in the UI.
     */
    public String displayName() {
        if (filename == null) {
            return "";
        }
        // Handle both forward and back slashes for cross-platform compatibility
        int lastSlash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        return lastSlash >= 0 ? filename.substring(lastSlash + 1) : filename;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String filename;
        private String title;
        private String date;
        private LocalDateTime lastModified;
        private String project;
        
        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder date(String date) {
            this.date = date;
            return this;
        }
        
        public Builder lastModified(LocalDateTime lastModified) {
            this.lastModified = lastModified;
            return this;
        }
        
        public Builder project(String project) {
            this.project = project;
            return this;
        }
        
        public Report build() {
            return new Report(filename, title, date, lastModified, project);
        }
    }
}
