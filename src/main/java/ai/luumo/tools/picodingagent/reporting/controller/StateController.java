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
        return ResponseEntity.ok(stateService.getState());
    }
    
    @PostMapping("/read/{filename}")
    public ResponseEntity<Void> markAsRead(@PathVariable String filename) {
        stateService.markAsRead(filename);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/unread/{filename}")
    public ResponseEntity<Void> markAsUnread(@PathVariable String filename) {
        stateService.markAsUnread(filename);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/flag/{filename}")
    public ResponseEntity<Void> toggleFlag(@PathVariable String filename) {
        stateService.toggleFlagged(filename);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/current")
    public ResponseEntity<Void> setCurrentReport(@RequestBody Map<String, String> body) {
        String filename = body.get("filename");
        if (filename != null) {
            stateService.setCurrentReport(filename);
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
    
    @PostMapping("/bell")
    public ResponseEntity<Void> setBell(@RequestBody Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        if (enabled != null) {
            stateService.setBellEnabled(enabled);
        }
        return ResponseEntity.ok().build();
    }
}
