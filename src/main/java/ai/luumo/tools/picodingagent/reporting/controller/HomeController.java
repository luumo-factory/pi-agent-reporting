package ai.luumo.tools.picodingagent.reporting.controller;

import ai.luumo.tools.picodingagent.reporting.config.BuildInfoProvider;
import ai.luumo.tools.picodingagent.reporting.service.StateService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    
    private final StateService stateService;
    private final BuildInfoProvider buildInfoProvider;
    
    public HomeController(StateService stateService, BuildInfoProvider buildInfoProvider) {
        this.stateService = stateService;
        this.buildInfoProvider = buildInfoProvider;
    }
    
    @GetMapping("/")
    public String index(Model model) {
        // Don't pre-populate reports - let JavaScript fetch them via AJAX
        model.addAttribute("reports", java.util.Collections.emptyList());
        model.addAttribute("currentReport", null);
        model.addAttribute("notificationMode", stateService.getNotificationMode());
        model.addAttribute("gitCommitId", buildInfoProvider.getGitCommitId());
        model.addAttribute("buildVersion", buildInfoProvider.getBuildVersion());
        return "index";
    }
}
