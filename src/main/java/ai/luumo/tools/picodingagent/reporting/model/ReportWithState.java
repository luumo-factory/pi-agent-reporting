package ai.luumo.tools.picodingagent.reporting.model;

/**
 * Report enriched with state information.
 * The filename field contains the relative path from reports root.
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
    
    public String getDisplayName() {
        return report.displayName();
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
    
    public String getProject() {
        return report.project();
    }
    
    public String getTimestampISO() {
        return report.getTimestampISO();
    }
    
    // State properties
    public boolean isRead() {
        return read;
    }
    
    public boolean isFlagged() {
        return flagged;
    }
}
