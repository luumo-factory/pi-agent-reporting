package ai.luumo.tools.picodingagent.reporting.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashSet;
import java.util.Set;

/**
 * Global application state - shared across all users/sessions.
 * All report identifiers (readReports, flaggedReports, currentReport) use relative paths
 * from the reports root directory (e.g., "project/report.md" or "report.md").
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationState {
    // Set of relative paths to reports marked as read
    private Set<String> readReports = new HashSet<>();
    // Set of relative paths to reports marked as flagged
    private Set<String> flaggedReports = new HashSet<>();
    // Relative path of the currently viewed report
    private String currentReport;
    private boolean autoReadEnabled = false;
    private String notificationMode = "bell";
    
    public Set<String> getReadReports() {
        return readReports;
    }
    
    public void setReadReports(Set<String> readReports) {
        this.readReports = readReports;
    }
    
    public Set<String> getFlaggedReports() {
        return flaggedReports;
    }
    
    public void setFlaggedReports(Set<String> flaggedReports) {
        this.flaggedReports = flaggedReports;
    }
    
    public String getCurrentReport() {
        return currentReport;
    }
    
    public void setCurrentReport(String currentReport) {
        this.currentReport = currentReport;
    }
    
    public boolean isAutoReadEnabled() {
        return autoReadEnabled;
    }
    
    public void setAutoReadEnabled(boolean autoReadEnabled) {
        this.autoReadEnabled = autoReadEnabled;
    }
    
    public String getNotificationMode() {
        return notificationMode;
    }
    
    public void setNotificationMode(String notificationMode) {
        // Validate mode
        if (notificationMode != null && 
            (notificationMode.equals("tts") || 
             notificationMode.equals("bell") || 
             notificationMode.equals("silence"))) {
            this.notificationMode = notificationMode;
        } else {
            // Default to bell if invalid
            this.notificationMode = "bell";
        }
    }
    
    /**
     * Check if a report is marked as read.
     * @param path Relative path from reports root (e.g., "project/report.md")
     */
    public boolean isRead(String path) {
        return readReports.contains(path);
    }
    
    /**
     * Check if a report is marked as flagged.
     * @param path Relative path from reports root (e.g., "project/report.md")
     */
    public boolean isFlagged(String path) {
        return flaggedReports.contains(path);
    }
    
    /**
     * Mark a report as read.
     * @param path Relative path from reports root (e.g., "project/report.md")
     */
    public void markAsRead(String path) {
        readReports.add(path);
    }
    
    /**
     * Mark a report as unread.
     * @param path Relative path from reports root (e.g., "project/report.md")
     */
    public void markAsUnread(String path) {
        readReports.remove(path);
    }
    
    /**
     * Toggle the flagged state of a report.
     * @param path Relative path from reports root (e.g., "project/report.md")
     */
    public void toggleFlagged(String path) {
        if (flaggedReports.contains(path)) {
            flaggedReports.remove(path);
        } else {
            flaggedReports.add(path);
        }
    }
}
