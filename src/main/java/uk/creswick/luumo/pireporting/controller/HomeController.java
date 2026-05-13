package uk.creswick.luumo.pireporting.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import uk.creswick.luumo.pireporting.service.ReportScannerService;

@Controller
public class HomeController {
    
    private static final Logger log = LoggerFactory.getLogger(HomeController.class);
    private final ReportScannerService reportScannerService;
    
    public HomeController(ReportScannerService reportScannerService) {
        this.reportScannerService = reportScannerService;
    }
    
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("reports", reportScannerService.getAllReports());
        return "index";
    }
}
