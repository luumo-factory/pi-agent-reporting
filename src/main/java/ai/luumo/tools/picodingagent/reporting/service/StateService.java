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
    private boolean modified = false;
    
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
        if (modified) {
            saveState();
        }
    }
    
    private void loadState() {
        lock.writeLock().lock();
        try {
            Path path = Paths.get(stateFile);
            if (Files.exists(path)) {
                ApplicationState loaded = objectMapper.readValue(path.toFile(), ApplicationState.class);
                state.setReadReports(loaded.getReadReports());
                state.setFlaggedReports(loaded.getFlaggedReports());
                state.setCurrentReport(loaded.getCurrentReport());
                state.setAutoReadEnabled(loaded.isAutoReadEnabled());
                state.setBellEnabled(loaded.isBellEnabled());
                log.info("Loaded state from {}: {} read, {} flagged", 
                    stateFile, state.getReadReports().size(), state.getFlaggedReports().size());
            } else {
                log.info("No state file found, using defaults");
            }
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
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), state);
            modified = false;
            log.debug("Saved state to {}", stateFile);
        } catch (IOException e) {
            log.error("Failed to save state to {}", stateFile, e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public ApplicationState getState() {
        lock.readLock().lock();
        try {
            return state;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void markAsRead(String filename) {
        lock.writeLock().lock();
        try {
            state.markAsRead(filename);
            modified = true;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void markAsUnread(String filename) {
        lock.writeLock().lock();
        try {
            state.markAsUnread(filename);
            modified = true;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void toggleFlagged(String filename) {
        lock.writeLock().lock();
        try {
            state.toggleFlagged(filename);
            modified = true;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void setCurrentReport(String filename) {
        lock.writeLock().lock();
        try {
            state.setCurrentReport(filename);
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
    
    public void setBellEnabled(boolean enabled) {
        lock.writeLock().lock();
        try {
            state.setBellEnabled(enabled);
            modified = true;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
