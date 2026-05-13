package ai.luumo.tools.picodingagent.reporting.service;

import ai.luumo.tools.picodingagent.reporting.model.ApplicationState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class StateService {
    
    private static final Logger log = LoggerFactory.getLogger(StateService.class);
    
    @Value("${app.state.file:state.json}")
    private String stateFile;
    
    private final ObjectMapper objectMapper;
    
    public StateService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    private final ApplicationState state = new ApplicationState();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile boolean modified = false;
    
    @PostConstruct
    public void init() {
        loadState();
    }
    
    @PreDestroy
    public void shutdown() {
        saveState();
    }
    
    /**
     * Auto-save state every 5 seconds if modified
     */
    @Scheduled(fixedDelay = 5000)
    public void autoSave() {
        if (!modified) {
            return;
        }
        saveState();
    }
    
    private void loadState() {
        lock.writeLock().lock();
        try {
            Path path = Paths.get(stateFile);
            if (Files.exists(path)) {
                ApplicationState loaded = objectMapper.readValue(path.toFile(), ApplicationState.class);
                state.setReadReports(new HashSet<>(loaded.getReadReports()));
                state.setFlaggedReports(new HashSet<>(loaded.getFlaggedReports()));
                state.setCurrentReport(loaded.getCurrentReport());
                state.setAutoReadEnabled(loaded.isAutoReadEnabled());
                state.setNotificationMode(loaded.getNotificationMode());
                log.info("Loaded state from {}: {} read, {} flagged", 
                    stateFile, state.getReadReports().size(), state.getFlaggedReports().size());
            } else {
                log.info("No state file found, using defaults");
            }
            modified = false;
        } catch (IOException e) {
            log.error("Failed to load state from {}", stateFile, e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void saveState() {
        lock.readLock().lock();
        try {
            Path path = Paths.get(stateFile);
            ensureParentDirectory(path);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), state);
            modified = false;
            log.debug("Saved state to {}", stateFile);
        } catch (IOException e) {
            log.error("Failed to save state to {}", stateFile, e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    private void ensureParentDirectory(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
    }
    
    public ApplicationState getStateSnapshot() {
        lock.readLock().lock();
        try {
            return copyState(state);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    private ApplicationState copyState(ApplicationState source) {
        ApplicationState snapshot = new ApplicationState();
        snapshot.setReadReports(new HashSet<>(source.getReadReports()));
        snapshot.setFlaggedReports(new HashSet<>(source.getFlaggedReports()));
        snapshot.setCurrentReport(source.getCurrentReport());
        snapshot.setAutoReadEnabled(source.isAutoReadEnabled());
        snapshot.setNotificationMode(source.getNotificationMode());
        return snapshot;
    }
    
    public boolean isReportRead(String path) {
        lock.readLock().lock();
        try {
            return state.isRead(path);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public boolean isReportFlagged(String path) {
        lock.readLock().lock();
        try {
            return state.isFlagged(path);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public String getNotificationMode() {
        lock.readLock().lock();
        try {
            return state.getNotificationMode();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Mark a report as read.
     * @param path Relative path from reports root (e.g., "project/report.md")
     */
    public void markAsRead(String path) {
        lock.writeLock().lock();
        try {
            state.markAsRead(path);
            modified = true;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Mark a report as unread.
     * @param path Relative path from reports root (e.g., "project/report.md")
     */
    public void markAsUnread(String path) {
        lock.writeLock().lock();
        try {
            state.markAsUnread(path);
            modified = true;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Toggle the flagged state of a report.
     * @param path Relative path from reports root (e.g., "project/report.md")
     */
    public void toggleFlagged(String path) {
        lock.writeLock().lock();
        try {
            state.toggleFlagged(path);
            modified = true;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Set the currently viewed report.
     * @param path Relative path from reports root (e.g., "project/report.md")
     */
    public void setCurrentReport(String path) {
        lock.writeLock().lock();
        try {
            state.setCurrentReport(path);
            modified = true;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void setAutoReadEnabled(boolean enabled) {
        lock.writeLock().lock();
        try {
            state.setAutoReadEnabled(enabled);
            modified = true;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void setNotificationMode(String mode) {
        lock.writeLock().lock();
        try {
            state.setNotificationMode(mode);
            modified = true;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
