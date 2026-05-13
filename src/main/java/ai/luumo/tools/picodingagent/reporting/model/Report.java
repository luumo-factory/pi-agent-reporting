package ai.luumo.tools.picodingagent.reporting.model;

import ai.luumo.tools.picodingagent.reporting.util.TimeFormatter;
import java.time.LocalDateTime;

public record Report(
    String filename,
    String title,
    String date,
    LocalDateTime lastModified,
    String type,
    String description
) {
    public boolean isMarkdown() {
        return "md".equalsIgnoreCase(type);
    }
    
    public boolean isHtml() {
        return "html".equalsIgnoreCase(type);
    }
    
    public String getRelativeTime() {
        return TimeFormatter.formatRelativeTime(lastModified);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String filename;
        private String title;
        private String date;
        private LocalDateTime lastModified;
        private String type;
        private String description;
        
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
        
        public Builder type(String type) {
            this.type = type;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Report build() {
            return new Report(filename, title, date, lastModified, type, description);
        }
    }
}
