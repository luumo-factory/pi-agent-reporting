package ai.luumo.tools.picodingagent.reporting.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashSet;
import java.util.Set;

/**
 * Global application state - shared across all users/sessions
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationState {
    private Set<String> readReports = new HashSet<>();
    private Set<String> flaggedReports = new HashSet<>();
    private String currentReport;
    private boolean autoReadEnabled = false;
    private boolean bellEnabled = true;
    
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
    
    public boolean isBellEnabled() {
        return bellEnabled;
    }
    
    public void setBellEnabled(boolean bellEnabled) {
        this.bellEnabled = bellEnabled;
    }
    
    public boolean isRead(String filename) {
        return readReports.contains(filename);
    }
    
    public boolean isFlagged(String filename) {
        return flaggedReports.contains(filename);
    }
    
    public void markAsRead(String filename) {
        readReports.add(filename);
    }
    
    public void markAsUnread(String filename) {
        readReports.remove(filename);
    }
    
    public void toggleFlagged(String filename) {
        if (flaggedReports.contains(filename)) {
            flaggedReports.remove(filename);
        } else {
            flaggedReports.add(filename);
        }
    }
}
