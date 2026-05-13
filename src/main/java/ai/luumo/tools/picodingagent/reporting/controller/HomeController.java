package ai.luumo.tools.picodingagent.reporting.controller;

import ai.luumo.tools.picodingagent.reporting.model.ReportWithState;
import ai.luumo.tools.picodingagent.reporting.service.StateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ai.luumo.tools.picodingagent.reporting.service.ReportScannerService;

import java.util.stream.Collectors;

@Controller
public class HomeController {
    
    private static final Logger log = LoggerFactory.getLogger(HomeController.class);
    private final ReportScannerService reportScannerService;
    private final StateService stateService;
    
    public HomeController(ReportScannerService reportScannerService, StateService stateService) {
        this.reportScannerService = reportScannerService;
        this.stateService = stateService;
    }
    
    @GetMapping("/")
    public String index(Model model) {
        // Don't pre-populate reports - let JavaScript fetch them via AJAX
        model.addAttribute("reports", java.util.Collections.emptyList());
        model.addAttribute("currentReport", null);
        model.addAttribute("notificationMode", stateService.getState().getNotificationMode());
        return "index";
    }
}
