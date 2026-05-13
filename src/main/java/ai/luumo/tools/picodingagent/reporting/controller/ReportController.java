package ai.luumo.tools.picodingagent.reporting.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ai.luumo.tools.picodingagent.reporting.model.Report;
import ai.luumo.tools.picodingagent.reporting.model.ReportWithState;
import ai.luumo.tools.picodingagent.reporting.service.MarkdownService;
import ai.luumo.tools.picodingagent.reporting.service.ReportScannerService;
import ai.luumo.tools.picodingagent.reporting.service.StateService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ReportController {
    
    private static final Logger log = LoggerFactory.getLogger(ReportController.class);
    private final ReportScannerService reportScannerService;
    private final MarkdownService markdownService;
    private final StateService stateService;
    
    public ReportController(ReportScannerService reportScannerService, 
                           MarkdownService markdownService,
                           StateService stateService) {
        this.reportScannerService = reportScannerService;
        this.markdownService = markdownService;
        this.stateService = stateService;
    }
    
    @GetMapping("/reports")
    public ResponseEntity<List<ReportWithState>> getReports() {
        List<ReportWithState> reportsWithState = reportScannerService.getAllReports().stream()
            .map(report -> new ReportWithState(
                report,
                stateService.getState().isRead(report.filename()),
                stateService.getState().isFlagged(report.filename())
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(reportsWithState);
    }
    
    @GetMapping(value = "/reports/{filename}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getReport(@PathVariable String filename) {
        try {
            Report report = reportScannerService.getReport(filename)
                .orElseThrow(() -> new RuntimeException("Report not found: " + filename));
            
            Path reportPath = reportScannerService.getReportPath(filename);
            
            if (!Files.exists(reportPath)) {
                log.error("Report file does not exist: {}", reportPath);
                return ResponseEntity.notFound().build();
            }
            
            String content = Files.readString(reportPath);
            
            // Convert markdown to HTML fragment
            String htmlContent = markdownService.markdownToHtml(content);
            String wrappedContent = markdownService.wrapInTemplate(
                htmlContent,
                report.title(),
                report.date(),
                "Pi Agent"
            );
            return ResponseEntity.ok(wrappedContent);
            
        } catch (IOException e) {
            log.error("Error reading report file: {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error loading report: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error processing report: {}", filename, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Report not found");
        }
    }
    
    @GetMapping(value = "/reports/{filename}/raw", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getRawReport(@PathVariable String filename) {
        try {
            Path reportPath = reportScannerService.getReportPath(filename);
            
            if (!Files.exists(reportPath)) {
                log.error("Report file does not exist: {}", reportPath);
                return ResponseEntity.notFound().build();
            }
            
            String content = Files.readString(reportPath);
            return ResponseEntity.ok(content);
            
        } catch (IOException e) {
            log.error("Error reading report file: {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error loading report: " + e.getMessage());
        }
    }
}
