package ai.luumo.tools.picodingagent.reporting.controller;

import ai.luumo.tools.picodingagent.reporting.model.ApplicationState;
import ai.luumo.tools.picodingagent.reporting.service.StateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/state")
public class StateController {
    
    private final StateService stateService;
    
    public StateController(StateService stateService) {
        this.stateService = stateService;
    }
    
    @GetMapping
    public ResponseEntity<ApplicationState> getState() {
        return ResponseEntity.ok(stateService.getStateSnapshot());
    }
    
    /**
     * Mark a report as read.
     * @param path Relative path from reports root (e.g., "project/report.md")
     *             Using {*path} to capture multi-segment paths in Spring Boot 3+.
     */
    @PostMapping("/read/{*path}")
    public ResponseEntity<Void> markAsRead(@PathVariable(name = "path") String pathParam) {
        // Remove leading slash from {*path} pattern
        String path = pathParam.startsWith("/") ? pathParam.substring(1) : pathParam;
        stateService.markAsRead(path);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Mark a report as unread.
     * @param path Relative path from reports root (e.g., "project/report.md")
     *             Using {*path} to capture multi-segment paths in Spring Boot 3+.
     */
    @PostMapping("/unread/{*path}")
    public ResponseEntity<Void> markAsUnread(@PathVariable(name = "path") String pathParam) {
        // Remove leading slash from {*path} pattern
        String path = pathParam.startsWith("/") ? pathParam.substring(1) : pathParam;
        stateService.markAsUnread(path);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Toggle the flagged state of a report.
     * @param path Relative path from reports root (e.g., "project/report.md")
     *             Using {*path} to capture multi-segment paths in Spring Boot 3+.
     */
    @PostMapping("/flag/{*path}")
    public ResponseEntity<Void> toggleFlag(@PathVariable(name = "path") String pathParam) {
        // Remove leading slash from {*path} pattern
        String path = pathParam.startsWith("/") ? pathParam.substring(1) : pathParam;
        stateService.toggleFlagged(path);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Set the currently viewed report.
     * Request body should contain: {"path": "project/report.md"}
     */
    @PostMapping("/current")
    public ResponseEntity<Void> setCurrentReport(@RequestBody Map<String, String> body) {
        String path = body.get("path");
        // Also support legacy "filename" key for backwards compatibility
        if (path == null) {
            path = body.get("filename");
        }
        if (path != null) {
            stateService.setCurrentReport(path);
        }
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/auto-read")
    public ResponseEntity<Void> setAutoRead(@RequestBody Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        if (enabled != null) {
            stateService.setAutoReadEnabled(enabled);
        }
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/notification-mode")
    public ResponseEntity<Void> setNotificationMode(@RequestBody Map<String, String> body) {
        String mode = body.get("mode");
        if (mode != null && 
            (mode.equals("tts") || mode.equals("bell") || mode.equals("silence"))) {
            stateService.setNotificationMode(mode);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }
}
