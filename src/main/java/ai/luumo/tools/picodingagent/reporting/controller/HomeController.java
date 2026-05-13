package ai.luumo.tools.picodingagent.reporting.controller;

import ai.luumo.tools.picodingagent.reporting.service.StateService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Controller
public class HomeController {
    
    private static final Logger log = LoggerFactory.getLogger(HomeController.class);
    private final StateService stateService;
    private String gitCommitId = "unknown";
    private String buildVersion = "1.0.0-SNAPSHOT";
    
    public HomeController(StateService stateService) {
        this.stateService = stateService;
    }
    
    @PostConstruct
    public void init() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("git.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                gitCommitId = props.getProperty("git.commit.id.abbrev", "unknown");
                buildVersion = props.getProperty("git.build.version", "1.0.0-SNAPSHOT");
                log.info("Loaded git info - commit: {}, version: {}", gitCommitId, buildVersion);
            } else {
                log.warn("git.properties not found, using defaults");
            }
        } catch (IOException e) {
            log.warn("Failed to load git.properties: {}", e.getMessage());
        }
    }
    
    @GetMapping("/")
    public String index(Model model) {
        // Don't pre-populate reports - let JavaScript fetch them via AJAX
        model.addAttribute("reports", java.util.Collections.emptyList());
        model.addAttribute("currentReport", null);
        model.addAttribute("notificationMode", stateService.getNotificationMode());
        model.addAttribute("gitCommitId", gitCommitId);
        model.addAttribute("buildVersion", buildVersion);
        return "index";
    }
}
