package ai.luumo.tools.picodingagent.reporting.controller;

import ai.luumo.tools.picodingagent.reporting.config.BuildInfoProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class ServiceWorkerController {

    private static final String TEMPLATE_PATH = "sw-template.js";

    private final BuildInfoProvider buildInfoProvider;
    private final String serviceWorkerTemplate;

    public ServiceWorkerController(BuildInfoProvider buildInfoProvider) {
        this.buildInfoProvider = buildInfoProvider;
        this.serviceWorkerTemplate = loadTemplate();
    }

    @GetMapping(value = "/sw.js", produces = "application/javascript")
    public ResponseEntity<String> serviceWorker() {
        final String version = buildInfoProvider.getGitCommitId();
        final String payload = serviceWorkerTemplate.replace("__APP_VERSION__", version);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .contentType(MediaType.valueOf("application/javascript"))
                .body(payload);
    }

    private String loadTemplate() {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        try (var inputStream = resource.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load service worker template", e);
        }
    }
}
