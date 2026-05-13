package ai.luumo.tools.picodingagent.reporting.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ai.luumo.tools.picodingagent.reporting.model.ApplicationState;
import ai.luumo.tools.picodingagent.reporting.model.Report;
import ai.luumo.tools.picodingagent.reporting.model.ReportWithState;
import ai.luumo.tools.picodingagent.reporting.service.MarkdownService;
import ai.luumo.tools.picodingagent.reporting.service.ReportScannerService;
import ai.luumo.tools.picodingagent.reporting.service.StateService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
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
        ApplicationState snapshot = stateService.getStateSnapshot();
        List<ReportWithState> reportsWithState = reportScannerService.getAllReports().stream()
            .map(report -> new ReportWithState(
                report,
                snapshot.isRead(report.filename()),
                snapshot.isFlagged(report.filename())
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(reportsWithState);
    }
    
    /**
     * Get a report's rendered HTML content.
     * @param path Relative path from reports root (e.g., "project/report.md")
     *             Spring Boot automatically URL-decodes the path parameter.
     *             Using {*path} to capture multi-segment paths in Spring Boot 3+.
     */
    @GetMapping(value = "/reports/html/{*path}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getReport(@PathVariable(name = "path") String pathParam) {
        try {
            ResolvedReport resolved = resolveReport(pathParam)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
            
            if (!Files.exists(resolved.path())) {
                log.error("Report file does not exist: {}", resolved.path());
                return ResponseEntity.notFound().build();
            }
            
            String content = Files.readString(resolved.path());
            
            String htmlContent = markdownService.markdownToHtml(content);
            String wrappedContent = markdownService.wrapInTemplate(
                htmlContent,
                resolved.report().title(),
                resolved.report().date(),
                "Pi Agent"
            );
            return ResponseEntity.ok(wrappedContent);
            
        } catch (IOException e) {
            log.error("Error reading report file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error loading report: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Report not found");
        }
    }
    
    /**
     * Get a report's raw markdown content.
     * @param path Relative path from reports root (e.g., "project/report.md")
     *             Spring Boot automatically URL-decodes the path parameter.
     *             Using {*path} to capture multi-segment paths in Spring Boot 3+.
     */
    @GetMapping(value = "/reports/raw/{*path}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getRawReport(@PathVariable(name = "path") String pathParam) {
        try {
            ResolvedReport resolved = resolveReport(pathParam)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
            
            if (!Files.exists(resolved.path())) {
                log.error("Report file does not exist: {}", resolved.path());
                return ResponseEntity.notFound().build();
            }
            
            String content = Files.readString(resolved.path());
            return ResponseEntity.ok(content);
            
        } catch (IOException e) {
            log.error("Error reading report file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error loading report: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Report not found");
        }
    }
    
    private Optional<ResolvedReport> resolveReport(String rawPath) {
        String sanitized = normalizeClientPath(rawPath);
        Optional<Report> optionalReport = reportScannerService.getReport(sanitized);
        if (optionalReport.isEmpty()) {
            return Optional.empty();
        }
        Report report = optionalReport.get();
        Path reportPath = reportScannerService.getReportPath(report.filename());
        Path rootPath = reportScannerService.getReportsRootPath();
        if (!reportPath.startsWith(rootPath)) {
            log.warn("Attempted access outside reports directory: {}", reportPath);
            return Optional.empty();
        }
        return Optional.of(new ResolvedReport(report, reportPath));
    }
    
    private String normalizeClientPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("Path must not be empty");
        }
        String trimmed = rawPath.replaceFirst("^/+", "");
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("Path must not be empty");
        }
        Path normalized = Paths.get(trimmed).normalize();
        String normalizedStr = normalized.toString().replace("\\", "/");
        if (normalizedStr.isEmpty() || normalizedStr.startsWith("..")) {
            throw new IllegalArgumentException("Invalid path");
        }
        return normalizedStr;
    }
    
    private record ResolvedReport(Report report, Path path) {}
}
