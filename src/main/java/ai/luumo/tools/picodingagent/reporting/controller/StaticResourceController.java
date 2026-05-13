package ai.luumo.tools.picodingagent.reporting.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Controller to serve static resources at legacy paths for backward compatibility
 * with existing HTML reports that reference ../report-engine/report-theme.css
 */
@Controller
public class StaticResourceController {
    
    @GetMapping(value = "/report-engine/report-theme.css", produces = "text/css")
    public ResponseEntity<String> getThemeCss() throws IOException {
        Resource resource = new ClassPathResource("static/css/report-theme.css");
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("text/css"))
                .body(content);
    }
    
    @GetMapping(value = "/report-engine/report-theme.js", produces = "application/javascript")
    public ResponseEntity<String> getThemeJs() throws IOException {
        Resource resource = new ClassPathResource("static/js/report-theme.js");
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/javascript"))
                .body(content);
    }
}
