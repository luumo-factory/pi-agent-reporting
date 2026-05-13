package ai.luumo.tools.picodingagent.reporting.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record Report(
    String filename,
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
