package ai.luumo.tools.picodingagent.reporting.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ai.luumo.tools.picodingagent.reporting.model.Report;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
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
    
    // Cache key is the relative path from reports root (e.g., "project/report.md")
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
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            int checked = 0;
            while ((line = reader.readLine()) != null && checked < 20) {
                String trimmed = line.trim();
                if (trimmed.startsWith("# ") && trimmed.length() > 2) {
                    return trimmed.substring(2).trim();
                }
                checked++;
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
    
    /**
     * Get a report by its relative path from the reports root.
     * @param path The relative path (e.g., "project/report.md" or "report.md")
     * @return Optional containing the report if found
     */
    public Optional<Report> getReport(String path) {
        return Optional.ofNullable(reportCache.get(path));
    }
    
    /**
     * Get the full filesystem path for a report given its relative path.
     * @param relativePath The relative path from reports root (e.g., "project/report.md")
     * @return The full filesystem path
     */
    public Path getReportPath(String relativePath) {
        Path root = getReportsRootPath();
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Resolved path escapes reports directory");
        }
        return resolved;
    }
    
    public Path getReportsRootPath() {
        return Paths.get(reportsDirectory).toAbsolutePath().normalize();
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
        // Calculate relative path from reports root directory
        Path relativePath = rootPath.relativize(filePath);
        // Use forward slashes consistently for web compatibility
        String relativePathStr = relativePath.toString().replace("\\", "/");
        currentFiles.add(relativePathStr);
        
        String filename = filePath.getFileName().toString();
        
        // Determine project from directory structure
        Path relativeParentPath = rootPath.relativize(filePath.getParent());
        String project = relativeParentPath.toString().isEmpty()
            ? "global"
            : normalizeProject(relativeParentPath.toString());
        
        LocalDateTime lastModified = LocalDateTime.ofInstant(
            Files.getLastModifiedTime(filePath).toInstant(),
            ZoneId.systemDefault()
        );
        

        
        Report existingReport = reportCache.get(relativePathStr);
        
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
                .filename(relativePathStr)  // Store full relative path
                .title(title)
                .date(date)
                .lastModified(lastModified)
                .project(project)
                .build();
            
            reportCache.put(relativePathStr, report);
            log.debug("Report {} updated in cache (project: {})", relativePathStr, project);
        }
    }
    
    private String normalizeProject(String rawProjectPath) {
        String normalized = rawProjectPath.replace("\\", "/");
        normalized = normalized.replaceAll("/{2,}", "/");
        return normalized.toLowerCase(Locale.ROOT);
    }
}
