package ai.luumo.tools.picodingagent.reporting.model;

/**
 * Report enriched with state information
 */
public class ReportWithState {
    private final Report report;
    private final boolean read;
    private final boolean flagged;
    
    public ReportWithState(Report report, boolean read, boolean flagged) {
        this.report = report;
        this.read = read;
        this.flagged = flagged;
    }
    
    // Delegate all Report methods
    public String getFilename() {
        return report.filename();
    }
    
    public String getTitle() {
        return report.title();
    }
    
    public String getDate() {
        return report.date();
    }
    
    public String getLastModified() {
        return report.lastModified().toString();
    }
    
    public String getType() {
        return report.type();
    }
    
    public String getDescription() {
        return report.description();
    }
    
    public String getRelativeTime() {
        return report.getRelativeTime();
    }
    
    public String getTimestampISO() {
        return report.getTimestampISO();
    }
    
    public boolean isHtml() {
        return report.isHtml();
    }
    
    public boolean isMarkdown() {
        return report.isMarkdown();
    }
    
    // State properties
    public boolean isRead() {
        return read;
    }
    
    public boolean isFlagged() {
        return flagged;
    }
}
