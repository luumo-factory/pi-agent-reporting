package uk.creswick.luumo.pireporting.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import uk.creswick.luumo.pireporting.service.ReportScannerService;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final ReportScannerService reportScannerService;
    
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("reports", reportScannerService.getAllReports());
        return "index";
    }
}
