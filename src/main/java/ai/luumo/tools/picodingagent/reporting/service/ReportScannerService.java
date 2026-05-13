package ai.luumo.tools.picodingagent.reporting.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ai.luumo.tools.picodingagent.reporting.model.Report;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ReportScannerService {
    
    private static final Logger log = LoggerFactory.getLogger(ReportScannerService.class);
    
    @Value("${app.reports.directory}")
    private String reportsDirectory;
    
    private final Map<String, Report> reportCache = new ConcurrentHashMap<>();
    private final Pattern filePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})_(.+)\\.md");
    
    @PostConstruct
    public void init() {
        log.info("Initializing Report Scanner Service");
        log.info("Monitoring directory: {}", reportsDirectory);
        scanReports();
    }
    
    @Scheduled(fixedDelayString = "${app.reports.scan-interval}")
    public void scanReports() {
        try {
            Path reportPath = Paths.get(reportsDirectory);
            
            if (!Files.exists(reportPath)) {
                log.warn("Reports directory does not exist: {}", reportsDirectory);
                return;
            }
            
            Set<String> currentFiles = new HashSet<>();
            
            // Scan recursively for all .md files
            scanDirectory(reportPath, reportPath, currentFiles);
                    
            
            // Remove deleted files from cache
            reportCache.keySet().retainAll(currentFiles);
            
            log.debug("Report cache contains {} reports", reportCache.size());
            
        } catch (IOException e) {
            log.error("Error scanning reports directory", e);
        }
    }
    
    private String formatTitle(String description) {
        // Convert hyphens and underscores to spaces, then title case each word
        String normalized = description.replace("-", " ").replace("_", " ");
        return Arrays.stream(normalized.split("\\s+"))
            .filter(word -> !word.isEmpty())
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
            .collect(Collectors.joining(" "));
    }
    
    private String extractTitleFromMarkdown(Path filePath) {
        try {
            List<String> lines = Files.readAllLines(filePath);
            // Look for the first H1 heading in the first 20 lines
            int linesToCheck = Math.min(20, lines.size());
            for (int i = 0; i < linesToCheck; i++) {
                String line = lines.get(i).trim();
                // Look for H1 heading
                if (line.startsWith("# ") && line.length() > 2) {
                    return line.substring(2).trim();
                }
            }
        } catch (IOException e) {
            log.debug("Could not read file for title extraction: {}", filePath);
        }
        return null;
    }
    
    public List<Report> getAllReports() {
        return reportCache.values().stream()
            .sorted(Comparator.comparing(Report::lastModified).reversed())
            .collect(Collectors.toList());
    }
    
    public Optional<Report> getReport(String filename) {
        return Optional.ofNullable(reportCache.get(filename));
    }
    
    public Path getReportPath(String filename) {
        Report report = reportCache.get(filename);
        if (report != null && report.project() != null && !"global".equals(report.project())) {
            return Paths.get(reportsDirectory, report.project(), filename);
        }
        return Paths.get(reportsDirectory, filename);
    }
    
    private void scanDirectory(Path rootPath, Path currentPath, Set<String> currentFiles) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    // Recursively scan subdirectories
                    scanDirectory(rootPath, entry, currentFiles);
                } else if (entry.toString().endsWith(".md")) {
                    processReportFile(rootPath, entry, currentFiles);
                }
            }
        }
    }
    
    private void processReportFile(Path rootPath, Path filePath, Set<String> currentFiles) throws IOException {
        String filename = filePath.getFileName().toString();
        currentFiles.add(filename);
        
        // Determine project from directory structure
        Path relativePath = rootPath.relativize(filePath.getParent());
        String project = relativePath.toString().isEmpty() ? "global" : relativePath.toString().toLowerCase();
        
        LocalDateTime lastModified = LocalDateTime.ofInstant(
            Files.getLastModifiedTime(filePath).toInstant(),
            ZoneId.systemDefault()
        );
        
        Report existingReport = reportCache.get(filename);
        
        // Only update if file is new or modified
        if (existingReport == null || !existingReport.lastModified().equals(lastModified)) {
            
            String title;
            String date;
            
            // Try to extract title from markdown H1 first
            String extractedTitle = extractTitleFromMarkdown(filePath);
            
            // Try to match date-prefixed format
            Matcher matcher = filePattern.matcher(filename);
            if (matcher.matches()) {
                // Standard date-prefixed reports: YYYY-MM-DD_description.md
                date = matcher.group(1);
                if (extractedTitle != null) {
                    title = extractedTitle;
                } else {
                    // Fallback to filename
                    String description = matcher.group(2);
                    title = formatTitle(description);
                }
            } else {
                // Non-standard filename
                if (extractedTitle != null) {
                    title = extractedTitle;
                } else {
                    // Fallback to filename as title
                    String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
                    title = formatTitle(nameWithoutExt);
                }
                date = lastModified.toLocalDate().toString();
            }
            
            Report report = Report.builder()
                .filename(filename)
                .title(title)
                .date(date)
                .lastModified(lastModified)
                .project(project)
                .build();
            
            reportCache.put(filename, report);
            log.debug("Report {} updated in cache (project: {})", filename, project);
        }
    }
}
