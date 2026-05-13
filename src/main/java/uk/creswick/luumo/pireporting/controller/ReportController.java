package uk.creswick.luumo.pireporting.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.creswick.luumo.pireporting.model.Report;
import uk.creswick.luumo.pireporting.service.MarkdownService;
import uk.creswick.luumo.pireporting.service.ReportScannerService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReportController {
    
    private static final Logger log = LoggerFactory.getLogger(ReportController.class);
    private final ReportScannerService reportScannerService;
    private final MarkdownService markdownService;
    
    public ReportController(ReportScannerService reportScannerService, MarkdownService markdownService) {
        this.reportScannerService = reportScannerService;
        this.markdownService = markdownService;
    }
    
    @GetMapping("/reports")
    public ResponseEntity<List<Report>> getReports() {
        return ResponseEntity.ok(reportScannerService.getAllReports());
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
            
            // If it's a markdown file, convert to HTML with template
            if (report.isMarkdown()) {
                String htmlContent = markdownService.markdownToHtml(content);
                String wrappedContent = markdownService.wrapInTemplate(
                    htmlContent,
                    report.title(),
                    report.date(),
                    "Pi Agent"
                );
                return ResponseEntity.ok(wrappedContent);
            } else {
                // HTML files are served as-is
                return ResponseEntity.ok(content);
            }
            
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
}
