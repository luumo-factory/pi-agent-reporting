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
    private final Pattern filePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})_(.+)\\.(html|md)");
    
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
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(reportPath, "*.{html,md}")) {
                for (Path entry : stream) {
                    String filename = entry.getFileName().toString();
                    currentFiles.add(filename);
                    
                    // Skip host-changes.html as it's not a report
                    if ("host-changes.html".equals(filename)) {
                        continue;
                    }
                    
                    Matcher matcher = filePattern.matcher(filename);
                    if (matcher.matches()) {
                        LocalDateTime lastModified = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(entry).toInstant(),
                            ZoneId.systemDefault()
                        );
                        
                        Report existingReport = reportCache.get(filename);
                        
                        // Only update if file is new or modified
                        if (existingReport == null || !existingReport.lastModified().equals(lastModified)) {
                            String date = matcher.group(1);
                            String description = matcher.group(2).replace("-", " ");
                            String type = matcher.group(3);
                            
                            String title = formatTitle(description);
                            
                            Report report = Report.builder()
                                .filename(filename)
                                .title(title)
                                .date(date)
                                .lastModified(lastModified)
                                .type(type)
                                .description(description)
                                .build();
                            
                            reportCache.put(filename, report);
                            log.debug("Report {} updated in cache", filename);
                        }
                    }
                }
            }
            
            // Remove deleted files from cache
            reportCache.keySet().retainAll(currentFiles);
            
            log.debug("Report cache contains {} reports", reportCache.size());
            
        } catch (IOException e) {
            log.error("Error scanning reports directory", e);
        }
    }
    
    private String formatTitle(String description) {
        // Convert hyphenated description to title case
        return Arrays.stream(description.split("-"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
            .collect(Collectors.joining(" "));
    }
    
    public List<Report> getAllReports() {
        return reportCache.values().stream()
            .sorted(Comparator.comparing(Report::date).reversed()
                .thenComparing(Report::filename))
            .collect(Collectors.toList());
    }
    
    public Optional<Report> getReport(String filename) {
        return Optional.ofNullable(reportCache.get(filename));
    }
    
    public Path getReportPath(String filename) {
        return Paths.get(reportsDirectory, filename);
    }
}
